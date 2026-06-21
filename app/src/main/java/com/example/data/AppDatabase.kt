package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FamilyMember::class,
        ExpenseRecord::class,
        UtilityBill::class,
        HealthCheckup::class,
        MedicinePurchase::class,
        Budget::class,
        AppLockSettings::class,
        RecurringBillTemplate::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun expenseRecordDao(): ExpenseRecordDao
    abstract fun utilityBillDao(): UtilityBillDao
    abstract fun healthCheckupDao(): HealthCheckupDao
    abstract fun medicinePurchaseDao(): MedicinePurchaseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun appLockSettingsDao(): AppLockSettingsDao
    abstract fun recurringBillTemplateDao(): RecurringBillTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "household_tracker_db"
                )
                .fallbackToDestructiveMigration() // Graceful reset for sandbox builds if schemas drift
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
