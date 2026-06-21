package com.example.data

import kotlinx.coroutines.flow.Flow

class HouseholdRepository(private val database: AppDatabase) {

    private val familyMemberDao = database.familyMemberDao()
    private val expenseRecordDao = database.expenseRecordDao()
    private val utilityBillDao = database.utilityBillDao()
    private val healthCheckupDao = database.healthCheckupDao()
    private val medicinePurchaseDao = database.medicinePurchaseDao()
    private val budgetDao = database.budgetDao()
    private val appLockSettingsDao = database.appLockSettingsDao()
    private val recurringBillTemplateDao = database.recurringBillTemplateDao()

    // Flow definitions (Reactive core stream getters)
    val allFamilyMembers: Flow<List<FamilyMember>> = familyMemberDao.getAllFamilyMembers()
    val allExpenseRecords: Flow<List<ExpenseRecord>> = expenseRecordDao.getAllExpenseRecords()
    val allUtilityBills: Flow<List<UtilityBill>> = utilityBillDao.getAllUtilityBills()
    val allHealthCheckups: Flow<List<HealthCheckup>> = healthCheckupDao.getAllHealthCheckups()
    val allMedicinePurchases: Flow<List<MedicinePurchase>> = medicinePurchaseDao.getAllMedicinePurchases()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val lockSettingsFlow: Flow<AppLockSettings?> = appLockSettingsDao.getLockSettingsFlow()
    val allTemplates: Flow<List<RecurringBillTemplate>> = recurringBillTemplateDao.getAllTemplates()

    // Suspend operations for writes
    suspend fun insertFamilyMember(member: FamilyMember) {
        familyMemberDao.insertFamilyMember(member)
    }

    suspend fun deleteFamilyMember(member: FamilyMember) {
        familyMemberDao.deleteFamilyMember(member)
    }

    suspend fun insertExpenseRecord(record: ExpenseRecord) {
        expenseRecordDao.insertExpenseRecord(record)
    }

    suspend fun deleteExpenseRecord(record: ExpenseRecord) {
        expenseRecordDao.deleteExpenseRecord(record)
    }

    fun getPriceHistoryOfItem(itemName: String): Flow<List<ExpenseRecord>> {
        return expenseRecordDao.getPriceHistoryOfItem(itemName)
    }

    suspend fun insertUtilityBill(bill: UtilityBill) {
        utilityBillDao.insertUtilityBill(bill)
    }

    suspend fun deleteUtilityBill(bill: UtilityBill) {
        utilityBillDao.deleteUtilityBill(bill)
    }

    suspend fun insertHealthCheckup(checkup: HealthCheckup) {
        healthCheckupDao.insertHealthCheckup(checkup)
    }

    suspend fun deleteHealthCheckup(checkup: HealthCheckup) {
        healthCheckupDao.deleteHealthCheckup(checkup)
    }

    suspend fun insertMedicinePurchase(purchase: MedicinePurchase) {
        medicinePurchaseDao.insertMedicinePurchase(purchase)
    }

    suspend fun deleteMedicinePurchase(purchase: MedicinePurchase) {
        medicinePurchaseDao.deleteMedicinePurchase(purchase)
    }

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun getLockSettings(): AppLockSettings? {
        return appLockSettingsDao.getLockSettings()
    }

    suspend fun insertLockSettings(settings: AppLockSettings) {
        appLockSettingsDao.insertLockSettings(settings)
    }

    suspend fun insertTemplate(template: RecurringBillTemplate) {
        recurringBillTemplateDao.insertTemplate(template)
    }

    suspend fun deleteTemplate(template: RecurringBillTemplate) {
        recurringBillTemplateDao.deleteTemplate(template)
    }
}
