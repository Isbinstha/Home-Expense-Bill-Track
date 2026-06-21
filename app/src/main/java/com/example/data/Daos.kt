package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMember)
}

@Dao
interface ExpenseRecordDao {
    @Query("SELECT * FROM expense_records ORDER BY purchaseDate DESC")
    fun getAllExpenseRecords(): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expense_records WHERE itemName = :itemName ORDER BY purchaseDate DESC")
    fun getPriceHistoryOfItem(itemName: String): Flow<List<ExpenseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseRecord(record: ExpenseRecord)

    @Delete
    suspend fun deleteExpenseRecord(record: ExpenseRecord)
}

@Dao
interface UtilityBillDao {
    @Query("SELECT * FROM utility_bills ORDER BY dueDate ASC")
    fun getAllUtilityBills(): Flow<List<UtilityBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilityBill(bill: UtilityBill)

    @Delete
    suspend fun deleteUtilityBill(bill: UtilityBill)
}

@Dao
interface HealthCheckupDao {
    @Query("SELECT * FROM health_checkups ORDER BY date DESC")
    fun getAllHealthCheckups(): Flow<List<HealthCheckup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthCheckup(checkup: HealthCheckup)

    @Delete
    suspend fun deleteHealthCheckup(checkup: HealthCheckup)
}

@Dao
interface MedicinePurchaseDao {
    @Query("SELECT * FROM medicine_purchases ORDER BY purchaseDate DESC")
    fun getAllMedicinePurchases(): Flow<List<MedicinePurchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicinePurchase(purchase: MedicinePurchase)

    @Delete
    suspend fun deleteMedicinePurchase(purchase: MedicinePurchase)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)
}

@Dao
interface AppLockSettingsDao {
    @Query("SELECT * FROM app_lock_settings WHERE id = 1 LIMIT 1")
    suspend fun getLockSettings(): AppLockSettings?

    @Query("SELECT * FROM app_lock_settings WHERE id = 1 LIMIT 1")
    fun getLockSettingsFlow(): Flow<AppLockSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockSettings(settings: AppLockSettings)
}

@Dao
interface RecurringBillTemplateDao {
    @Query("SELECT * FROM recurring_bill_templates ORDER BY id ASC")
    fun getAllTemplates(): Flow<List<RecurringBillTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RecurringBillTemplate)

    @Delete
    suspend fun deleteTemplate(template: RecurringBillTemplate)
}
