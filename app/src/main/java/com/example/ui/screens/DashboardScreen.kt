package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.HouseholdViewModel
import com.example.ui.viewmodel.InAppReminder
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: HouseholdViewModel,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    val expenses by viewModel.expenseRecords.collectAsState()
    val bills by viewModel.utilityBills.collectAsState()
    val health by viewModel.healthCheckups.collectAsState()
    val medicines by viewModel.medicinePurchases.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val reminders by viewModel.pendingReminders.collectAsState()
    val curMonth by viewModel.currentMonthIdx.collectAsState()
    val curYear by viewModel.currentYearVal.collectAsState()

    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    // Aggregate monthly statistics
    val currentMonthExpenses = expenses.filter {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = it.purchaseDate }
        c.get(java.util.Calendar.MONTH) + 1 == curMonth && c.get(java.util.Calendar.YEAR) == curYear
    }
    val previousMonthExpenses = expenses.filter {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = it.purchaseDate }
        val checkMonth = if (curMonth == 1) 12 else curMonth - 1
        val checkYear = if (curMonth == 1) curYear - 1 else curYear
        c.get(java.util.Calendar.MONTH) + 1 == checkMonth && c.get(java.util.Calendar.YEAR) == checkYear
    }

    val totalMonthSpent = currentMonthExpenses.sumOf { it.totalAmount }
    val totalPrevMonthSpent = previousMonthExpenses.sumOf { it.totalAmount }

    val totalYearSpent = expenses.filter {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = it.purchaseDate }
        c.get(java.util.Calendar.YEAR) == curYear
    }.sumOf { it.totalAmount }

    val totalMedicineSpent = medicines.filter {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = it.purchaseDate }
        c.get(java.util.Calendar.MONTH) + 1 == curMonth && c.get(java.util.Calendar.YEAR) == curYear
    }.sumOf { it.totalAmount }

    val totalCheckupSpent = health.filter {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
        c.get(java.util.Calendar.MONTH) + 1 == curMonth && c.get(java.util.Calendar.YEAR) == curYear
    }.sumOf { it.totalCost }

    val unpaidBills = bills.filter { it.status == "Unpaid" }
    val totalUnpaidBillAmount = unpaidBills.sumOf { it.amount }

    // Overall Budget limit check
    val overallBudget = budgets.find { it.category == "Overall" && it.month == curMonth && it.year == curYear }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card / Banner
        item {
            Spacer(modifier = Modifier.height(8.dp))
            DashboardWelcomeHeader(viewModel)
        }

        // Quick KPI Summaries grid
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                val spentDeltaPct = if (totalPrevMonthSpent > 0) {
                    ((totalMonthSpent - totalPrevMonthSpent) / totalPrevMonthSpent) * 100
                } else 0.0

                KpiCard(
                    title = "Spent This Month",
                    value = "$symbol ${totalMonthSpent.toInt()}",
                    desc = if (spentDeltaPct > 0) "+%.1f%% than last mo".format(spentDeltaPct) else if (spentDeltaPct < 0) "%.1f%% less than last mo".format(spentDeltaPct) else "Steady",
                    icon = Icons.Default.Wallet,
                    gradient = Brush.linearGradient(listOf(Color(0xFF005AC1), Color(0xFF4B93FF))),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("month_spent_kpi")
                )

                KpiCard(
                    title = "Pending Utility Bills",
                    value = "$symbol ${totalUnpaidBillAmount.toInt()}",
                    desc = "${unpaidBills.size} bills require payment",
                    icon = Icons.Default.ReceiptLong,
                    gradient = Brush.linearGradient(listOf(Color(0xFFBA1A1A), Color(0xFFFF5449))),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_bills_kpi")
                )
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                val totalHealthCost = totalMedicineSpent + totalCheckupSpent
                KpiCard(
                    title = "Medical & Health",
                    value = "$symbol ${totalHealthCost.toInt()}",
                    desc = "Checkups + Medicines",
                    icon = Icons.Default.LocalHospital,
                    gradient = Brush.linearGradient(listOf(Color(0xFF7C51A1), Color(0xFFB389DC))),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("health_cost_kpi")
                )

                KpiCard(
                    title = "Yearly Expenses",
                    value = "$symbol ${totalYearSpent.toInt()}",
                    desc = "Total in $curYear",
                    icon = Icons.Default.CalendarToday,
                    gradient = Brush.linearGradient(listOf(Color(0xFF44474E), Color(0xFF8A8E9A))),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("year_spent_kpi")
                )
            }
        }

        // Budget Progress widget
        overallBudget?.let { b ->
            item {
                BudgetProgressCard(spent = totalMonthSpent, limit = b.budgetLimit, symbol = symbol)
            }
        }

        // Quick Hubs (Replacing generic Quick Actions)
        item {
            Text(
                text = "Quick Hubs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            val currentMonthGrocerySpent = currentMonthExpenses.filter { it.category == "Grocery" }.sumOf { it.totalAmount }
            val unpaidBillsCount = unpaidBills.size
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickModuleCard(
                        title = "Groceries",
                        subtitle = "$symbol ${currentMonthGrocerySpent.toInt()} Spent",
                        containerColor = GroceriesContainer,
                        badgeColor = GroceriesPurple,
                        icon = Icons.Default.ShoppingCart,
                        onClick = { viewModel.showAddExpenseDialog.value = true },
                        modifier = Modifier.weight(1f).testTag("action_add_grocery")
                    )
                    
                    QuickModuleCard(
                        title = "Utilities",
                        subtitle = "$unpaidBillsCount Unpaid Bills",
                        containerColor = UtilitiesContainer,
                        badgeColor = UtilitiesBlue,
                        icon = Icons.Default.OfflineBolt,
                        onClick = { viewModel.showAddBillDialog.value = true },
                        modifier = Modifier.weight(1f).testTag("action_add_bill")
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickModuleCard(
                        title = "Health",
                        subtitle = "Add Medical Log",
                        containerColor = HealthContainer,
                        badgeColor = HealthRed,
                        icon = Icons.Default.Healing,
                        onClick = { viewModel.showAddCheckupDialog.value = true },
                        modifier = Modifier.weight(1f).testTag("action_add_checkup")
                    )
                    
                    QuickModuleCard(
                        title = "Analytics",
                        subtitle = "Insights Ready",
                        containerColor = AnalyticsContainer,
                        badgeColor = AnalyticsGray,
                        icon = Icons.Default.BarChart,
                        onClick = { onNavigate("insights") },
                        modifier = Modifier.weight(1f).testTag("action_view_insights")
                    )
                }
            }
        }

        // Alert notifications feed (Reactive core)
        if (reminders.isNotEmpty()) {
            item {
                Text(
                    text = "Active Reminders & Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(reminders.take(3), key = { it.id }) { reminder ->
                ReminderItemRow(reminder)
            }
        }

        // Recent purchases log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Household Purchases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigate("groceries") }
                )
            }
        }

        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No purchases tracked yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Use Quick Actions above to start logging expenses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            items(expenses.take(4), key = { it.id }) { expense ->
                RecentTransactionRow(expense, viewModel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DashboardWelcomeHeader(viewModel: HouseholdViewModel) {
    val members by viewModel.familyMembers.collectAsState()
    val activeUser by viewModel.currentProfileName.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HOUSEHOLD MEMBERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (members.isEmpty()) {
                    Text(
                        text = "Manage profiles securely inside settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { member ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(member.badgeColorHex)).copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = member.name,
                                    color = Color(android.graphics.Color.parseColor(member.badgeColorHex)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    desc: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(115.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetProgressCard(spent: Double, limit: Double, symbol: String = "Rs.") {
    val pct = if (limit > 0) (spent / limit) else 0.0
    val pctString = "${(pct * 100).toInt()}%"
    
    // Format Month
    val cal = java.util.Calendar.getInstance()
    val monthNames = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
    val monthStr = monthNames.getOrNull(cal.get(java.util.Calendar.MONTH)) ?: "MON"
    val yearStr = cal.get(java.util.Calendar.YEAR).toString()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_progress_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFADC6FF).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Monthly Spend Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF001D49)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$symbol %,.2f".format(spent),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001945),
                        fontSize = 32.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$monthStr $yearStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF001945),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar matches design exactly
            LinearProgressIndicator(
                progress = pct.toFloat().coerceAtMost(1f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFADC6FF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Used: $pctString",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF001D49)
                )
                Text(
                    text = "Limit: $symbol %,.2f".format(limit),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF001D49)
                )
            }
        }
    }
}

@Composable
fun QuickModuleCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    badgeColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF44474E),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = { Text(text, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.testTag("action_${text.lowercase().replace(" ", "_")}")
    )
}

@Composable
fun ReminderItemRow(reminder: InAppReminder) {
    val bgColor = if (reminder.isOverdue) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
    val tintColor = if (reminder.isOverdue) Color(0xFFC62828) else Color(0xFFEF6C00)
    val icon = when (reminder.type) {
        "Bill Due" -> Icons.Default.Warning
        "Health Visit" -> Icons.Default.MedicalServices
        else -> Icons.Default.Notifications
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_${reminder.id}"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun RecentTransactionRow(
    expense: com.example.data.ExpenseRecord,
    viewModel: HouseholdViewModel
) {
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(expense.category).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = null,
                        tint = getCategoryColor(expense.category),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${expense.quantity} ${expense.unit} @ $symbol${expense.rate.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = expense.purchasedForName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$symbol ${expense.totalAmount.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = viewModel.formatDate(expense.purchaseDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Grocery" -> Icons.Default.ShoppingCart
        "Clothes" -> Icons.Default.Checkroom
        "Slippers" -> Icons.Default.DryCleaning // nearest logic or style
        "Personal" -> Icons.Default.Face
        "Health" -> Icons.Default.LocalHospital
        "Utility" -> Icons.Default.OfflineBolt
        else -> Icons.Default.Category
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Grocery" -> GroceriesPurple
        "Health" -> HealthRed
        "Utility" -> UtilitiesBlue
        "Personal" -> Color(0xFF00A2C9)
        "Clothes" -> Color(0xFFFF527A)
        "Slippers" -> Color(0xFFFF8B1F)
        else -> Color(0xFF8A8E9A)
    }
}
