package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val relation: String,
    val notes: String = "",
    val badgeColorHex: String = "#FF6200EE" // Random/chosen HEX color for visual badges
)

@Entity(tableName = "expense_records")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Grocery", "Clothes", "Slippers", "Personal", "Other"
    val itemName: String,
    val quantity: Double,
    val unit: String, // "kg", "liter", "piece", "packet", "dozen", "pcs", etc.
    val rate: Double,
    val totalAmount: Double, // calculated: rate * quantity
    val purchasedForName: String, // Tagged family member or "Everyone"
    val purchaseDate: Long, // timestamp in ms
    val vendorName: String = "",
    val notes: String = "",
    val paymentMethod: String = "Cash" // "Cash", "Online", "Credit"
)

@Entity(tableName = "utility_bills")
data class UtilityBill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val billType: String, // "Electricity", "Water", "Gas Cylinder", "Internet", "Mobile", "TV"
    val billingMonth: Int, // 1 to 12
    val billingYear: Int,
    val amount: Double,
    val dueDate: Long, // timestamp
    val paymentDate: Long? = null, // timestamp if paid, null if unpaid
    val status: String = "Unpaid", // "Paid", "Unpaid"
    val unitsConsumed: Double = 0.0, // electricity kwh, water kilolitres etc.
    val referenceNumber: String = "",
    val notes: String = ""
)

@Entity(tableName = "health_checkups")
data class HealthCheckup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String, // Tagged family member or "Self"
    val date: Long,
    val hospitalOrClinic: String,
    val checkupType: String, // "Routine", "Dental", "Cardiology", "Eye Exam", "Lab Test", etc.
    val doctorName: String = "",
    val diagnosis: String = "",
    val notes: String = "",
    val totalCost: Double,
    val followUpDate: Long? = null // Reminder date for follow up
)

@Entity(tableName = "medicine_purchases")
data class MedicinePurchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientName: String,
    val medicineName: String,
    val purchaseDate: Long,
    val quantity: Int,
    val rate: Double,
    val totalAmount: Double,
    val purpose: String, // "Fever", "Cough", "Blood Pressure", "Diabetes", "Vitamin", "Pain Relief", etc.
    val isPrescribed: Boolean = false,
    val notes: String = "",
    val linkedCheckupId: Int? = null // Optional relation to visual checkup
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Overall", "Grocery", "Utility", "Health", "Clothes", "Personal"
    val month: Int, // 1 - 12
    val year: Int,
    val budgetLimit: Double
)

@Entity(tableName = "app_lock_settings")
data class AppLockSettings(
    @PrimaryKey val id: Int = 1, // Single row config
    val pinCode: String = "", // Empty means disabled
    val isBiometricEnabled: Boolean = false,
    val securityQuestion: String = "",
    val securityAnswer: String = ""
)

@Entity(tableName = "recurring_bill_templates")
data class RecurringBillTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val billType: String, // "Electricity", "Water", "Gas Cylinder", "Internet", "Mobile", "TV"
    val defaultAmount: Double,
    val dueDayOfMonth: Int, // Day of the month: 1 - 31
    val usePreviousAmount: Boolean = true, // Generate based on most recent previous payment's amount
    val notes: String = ""
)

