package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HouseholdViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HouseholdRepository
    private val prefs = application.getSharedPreferences("household_prefs", Context.MODE_PRIVATE)

    val currentCurrency = MutableStateFlow(prefs.getString("currency", "NPR (Rs.)") ?: "NPR (Rs.)")
    val groceryAlertFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HouseholdRepository(database)
        createNotificationChannel()
        
        // Auto-run recurring bills generator on startup
        viewModelScope.launch {
            // Wait for database flows to yield cached data first
            kotlinx.coroutines.delay(1000)
            generateTemplateBills()
        }
    }

    fun setCurrency(currency: String) {
        currentCurrency.value = currency
        prefs.edit().putString("currency", currency).apply()
    }

    fun getCurrencySymbol(): String {
        return when (currentCurrency.value) {
            "USD ($)" -> "$"
            "EUR (€)" -> "€"
            "GBP (£)" -> "£"
            "INR (₹)" -> "₹"
            else -> "Rs."
        }
    }

    fun formatCurrency(amount: Double): String {
        val symbol = getCurrencySymbol()
        return "$symbol %,.2f".format(amount)
    }

    fun formatCurrencyShort(amount: Double): String {
        val symbol = getCurrencySymbol()
        return "$symbol ${amount.toInt()}"
    }

    // Repository flows
    val familyMembers = repository.allFamilyMembers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenseRecords = repository.allExpenseRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val utilityBills = repository.allUtilityBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val healthCheckups = repository.allHealthCheckups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val medicinePurchases = repository.allMedicinePurchases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.allBudgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lockSettings = repository.lockSettingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val templates = repository.allTemplates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App screen locks & profiles
    val isAppLocked = MutableStateFlow(true) // Start locked, can unlock in PIN entry
    val currentProfileName = MutableStateFlow("Primary Owner")

    // Navigation and screen management
    val currentScreen = MutableStateFlow("dashboard") // "dashboard", "groceries", "health", "bills", "clothes", "calendar", "insights", "budget", "profile"

    // Search & filter states
    val expenseSearchQuery = MutableStateFlow("")
    val expenseCategoryFilter = MutableStateFlow<String?>(null)
    val expenseFamilyFilter = MutableStateFlow<String?>(null)

    val selectedPriceHistoryItem = MutableStateFlow<String>("")

    // Computed Timeframes
    private val calendar = Calendar.getInstance()
    val currentMonthIdx = MutableStateFlow(calendar.get(Calendar.MONTH) + 1) // 1-12
    val currentYearVal = MutableStateFlow(calendar.get(Calendar.YEAR))

    // Form modal controllers
    val showAddExpenseDialog = MutableStateFlow(false)
    val showAddCheckupDialog = MutableStateFlow(false)
    val showAddMedicineDialog = MutableStateFlow(false)
    val showAddBillDialog = MutableStateFlow(false)
    val showAddBudgetDialog = MutableStateFlow(false)
    val showAddMemberDialog = MutableStateFlow(false)

    // Backups & Drive mock state
    val googleDriveConnected = MutableStateFlow(false)
    val lastBackupTimestamp = MutableStateFlow<String?>("Never")
    val autoBackupEnabled = MutableStateFlow(false)

    private val monthYearFlow = combine(currentMonthIdx, currentYearVal) { m, y -> Pair(m, y) }
    private val healthFlow = combine(medicinePurchases, healthCheckups) { meds, checkups -> Pair(meds, checkups) }

    // Reminders data derived reactively from Database records
    val pendingReminders = combine(
        utilityBills,
        healthFlow,
        budgets,
        expenseRecords,
        monthYearFlow
    ) { bills: List<UtilityBill>, health: Pair<List<MedicinePurchase>, List<HealthCheckup>>, budgetsList: List<Budget>, expenses: List<ExpenseRecord>, monthYear: Pair<Int, Int> ->
        val meds = health.first
        val checkups = health.second
        val month = monthYear.first
        val year = monthYear.second
        val list = mutableListOf<InAppReminder>()
        val now = System.currentTimeMillis()

        // 1. Unpaid utility bills due in future or overdue
        bills.forEach { bill ->
            if (bill.status != "Paid") {
                val daysDiff = ((bill.dueDate - now) / (1000 * 60 * 60 * 24)).toInt()
                val title = "${bill.billType} Utility Bill"
                val symbol = getCurrencySymbol()
                val desc = if (daysDiff < 0) {
                    "Overdue by ${-daysDiff} day(s)! Amount: $symbol ${bill.amount}"
                } else if (daysDiff == 0) {
                    "Due TODAY! Amount: $symbol ${bill.amount}"
                } else {
                    "Due in $daysDiff day(s) on ${formatDate(bill.dueDate)}. Amount: $symbol ${bill.amount}"
                }
                list.add(
                    InAppReminder(
                        id = "bill_${bill.id}",
                        type = "Bill Due",
                        title = title,
                        description = desc,
                        time = bill.dueDate,
                        isOverdue = daysDiff < 0
                    )
                )
            }
        }

        // 2. Health checkup follow ups
        checkups.forEach { cu ->
            cu.followUpDate?.let { fDate ->
                if (fDate >= now - (1000 * 60 * 60 * 24)) { // Up to 1 day in the past or in future
                    val daysDiff = ((fDate - now) / (1000 * 60 * 60 * 24)).toInt()
                    val title = "Checkup Follow-up: ${cu.patientName}"
                    val desc = if (daysDiff < 0) {
                        "Yesterday for ${cu.diagnosis} with Dr. ${cu.doctorName}"
                    } else if (daysDiff == 0) {
                        "TODAY! Visit ${cu.hospitalOrClinic} for ${cu.diagnosis}"
                    } else {
                        "In $daysDiff day(s) on ${formatDate(fDate)} for ${cu.diagnosis}"
                    }
                    list.add(
                        InAppReminder(
                            id = "checkup_${cu.id}",
                            type = "Health Visit",
                            title = title,
                            description = desc,
                            time = fDate,
                            isOverdue = daysDiff < 0
                        )
                    )
                }
            }
        }

        // 3. Budgets reached limits
        val monthExpenses = expenses.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.purchaseDate }
            c.get(Calendar.MONTH) + 1 == month && c.get(Calendar.YEAR) == year
        }
        val categoryGrouped = monthExpenses.groupBy { it.category }

        budgetsList.filter { it.month == month && it.year == year }.forEach { budget ->
            val spent = if (budget.category == "Overall") {
                monthExpenses.sumOf { it.totalAmount }
            } else {
                categoryGrouped[budget.category]?.sumOf { it.totalAmount } ?: 0.0
            }

            val pct = if (budget.budgetLimit > 0) (spent / budget.budgetLimit) * 100.0 else 0.0
            if (pct >= 80.0) {
                val isExceeded = pct >= 100.0
                val title = if (isExceeded) "Budget Exceeded!" else "Budget Warning (80% reached)"
                val symbol = getCurrencySymbol()
                val desc = "You spent $symbol ${spent.toInt()} of $symbol ${budget.budgetLimit.toInt()} under ${budget.category} category."
                list.add(
                    InAppReminder(
                        id = "budget_${budget.id}",
                        type = "Budget Alert",
                        title = title,
                        description = desc,
                        time = now,
                        isOverdue = isExceeded
                    )
                )
            }
        }

        list.sortedBy { it.time }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database insertions/deletions wrapped in viewModelScope
    fun addFamilyMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.insertFamilyMember(member)
        }
    }

    fun removeFamilyMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteFamilyMember(member)
        }
    }

    fun addExpenseRecord(record: ExpenseRecord) {
        viewModelScope.launch {
            repository.insertExpenseRecord(record)
            triggerCheckForAlerts(record)
        }
    }

    private fun triggerCheckForAlerts(record: ExpenseRecord) {
        viewModelScope.launch {
            if (record.category.equals("Grocery", ignoreCase = true)) {
                // Get all history for this item
                val historyFlow = repository.getPriceHistoryOfItem(record.itemName)
                val history = historyFlow.firstOrNull() ?: emptyList()
                
                // Element 0 is the current one we just inserted, so the previous one is at index 1
                if (history.size >= 2) {
                    val previous = history[1]
                    if (previous.rate > 0) {
                        val percentIncrease = ((record.rate - previous.rate) / previous.rate) * 100
                        if (percentIncrease >= 10.0) {
                            val symbol = getCurrencySymbol()
                            val msg = "Price for '${record.itemName}' increased by %.1f%% ($symbol%.1f -> $symbol%.1f) vs last purchase.".format(
                                percentIncrease, previous.rate, record.rate
                            )
                            fireLocalNotification("Significant Grocery Price Alert", msg)
                            groceryAlertFlow.emit(msg)
                        }
                    }
                }
            }
        }
    }

    fun addTemplate(template: RecurringBillTemplate) {
        viewModelScope.launch {
            repository.insertTemplate(template)
            generateTemplateBills()
        }
    }

    fun removeTemplate(template: RecurringBillTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun generateTemplateBills() {
        viewModelScope.launch {
            val activeBills = repository.allUtilityBills.firstOrNull() ?: emptyList()
            val activeTemplates = repository.allTemplates.firstOrNull() ?: emptyList()
            
            val calendar = Calendar.getInstance()
            val curMonth = calendar.get(Calendar.MONTH) + 1
            val curYear = calendar.get(Calendar.YEAR)
            
            activeTemplates.forEach { template ->
                val alreadyExists = activeBills.any { 
                    it.billType == template.billType && 
                    it.billingMonth == curMonth && 
                    it.billingYear == curYear
                }
                
                if (!alreadyExists) {
                    // Find most recent previous payment
                    val previousBills = activeBills.filter { it.billType == template.billType && it.status == "Paid" }
                        .sortedWith(compareByDescending<UtilityBill> { it.billingYear }.thenByDescending { it.billingMonth })
                    
                    val amountToUse = if (template.usePreviousAmount && previousBills.isNotEmpty()) {
                        previousBills.first().amount
                    } else {
                        template.defaultAmount
                    }
                    
                    val dueCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, curYear)
                        set(Calendar.MONTH, curMonth - 1)
                        set(Calendar.DAY_OF_MONTH, template.dueDayOfMonth.coerceIn(1, 28))
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val generatedBill = UtilityBill(
                        billType = template.billType,
                        billingMonth = curMonth,
                        billingYear = curYear,
                        amount = amountToUse,
                        dueDate = dueCal.timeInMillis,
                        status = "Pending Confirmation", // Visual confirmation status
                        unitsConsumed = 0.0,
                        notes = "Generated from Template"
                    )
                    
                    repository.insertUtilityBill(generatedBill)
                    
                    fireLocalNotification(
                        "New Recurring Bill Generated",
                        "${template.billType} bill for ${getMonthName(curMonth)} $curYear has been pre-generated. Confirm to activate."
                    )
                }
            }
        }
    }

    fun removeExpenseRecord(record: ExpenseRecord) {
        viewModelScope.launch {
            repository.deleteExpenseRecord(record)
        }
    }

    fun addUtilityBill(bill: UtilityBill) {
        viewModelScope.launch {
            repository.insertUtilityBill(bill)
        }
    }

    fun removeUtilityBill(bill: UtilityBill) {
        viewModelScope.launch {
            repository.deleteUtilityBill(bill)
        }
    }

    fun addHealthCheckup(checkup: HealthCheckup) {
        viewModelScope.launch {
            repository.insertHealthCheckup(checkup)
        }
    }

    fun removeHealthCheckup(checkup: HealthCheckup) {
        viewModelScope.launch {
            repository.deleteHealthCheckup(checkup)
        }
    }

    fun addMedicinePurchase(purchase: MedicinePurchase) {
        viewModelScope.launch {
            repository.insertMedicinePurchase(purchase)
        }
    }

    fun removeMedicinePurchase(purchase: MedicinePurchase) {
        viewModelScope.launch {
            repository.deleteMedicinePurchase(purchase)
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.insertBudget(budget)
        }
    }

    fun removeBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun updateLockSettings(pin: String, securityQ: String, securityA: String) {
        viewModelScope.launch {
            val settings = AppLockSettings(1, pin, pin.isNotEmpty(), securityQ, securityA)
            repository.insertLockSettings(settings)
            // If lock is cleared, unlock app
            if (pin.isEmpty()) {
                isAppLocked.value = false
            }
        }
    }

    // Google Drive Sync Mock
    fun triggerGoogleDriveBackup() {
        if (!googleDriveConnected.value) return
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            lastBackupTimestamp.value = sdf.format(Date())
            fireLocalNotification(
                "Backup Successful",
                "Stored local encrypted household records to your Drive profile successfully."
            )
        }
    }

    // Standard Android Local System Notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Household Budget Alert Channel"
            val descriptionText = "Handles reminders, due dates and spending budget alerts offline"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("HH_BUDGET_ALERTS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun fireLocalNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val builder = NotificationCompat.Builder(context, "HH_BUDGET_ALERTS")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random().nextInt(100000), builder.build())
    }

    // Helpers
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> ""
        }
    }
}

data class InAppReminder(
    val id: String,
    val type: String, // "Bill Due", "Health Visit", "Budget Alert"
    val title: String,
    val description: String,
    val time: Long,
    val isOverdue: Boolean
)
