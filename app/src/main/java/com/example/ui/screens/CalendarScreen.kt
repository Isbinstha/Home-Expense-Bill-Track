package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.HouseholdViewModel
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: HouseholdViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenseRecords.collectAsState()
    val bills by viewModel.utilityBills.collectAsState()
    val checkups by viewModel.healthCheckups.collectAsState()
    val medicines by viewModel.medicinePurchases.collectAsState()

    var calendarMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-11
    var calendarYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    var selectedDay by remember { mutableStateOf<Int?>(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    // Navigation triggers
    val daysInMonthStream = remember(calendarMonth, calendarYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, calendarMonth)
            set(Calendar.YEAR, calendarYear)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val emptyDaysBefore = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sun) to 6 (Sat)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        Pair(emptyDaysBefore, maxDays)
    }

    val emptyDaysBefore = daysInMonthStream.first
    val maxDays = daysInMonthStream.second

    // Filter categories state for Agenda list & Dots
    var calendarFilterType by remember { mutableStateOf("All") } // "All", "Grocery", "Utility", "Health", "Clothes"

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))

        // Month Selector Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (calendarMonth == 0) {
                        calendarMonth = 11
                        calendarYear -= 1
                    } else {
                        calendarMonth -= 1
                    }
                    selectedDay = 1
                }, modifier = Modifier.testTag("prev_month")) {
                    Icon(Icons.Default.ArrowBackIos, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Text(
                    text = "${getMonthName(calendarMonth + 1)} $calendarYear",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                IconButton(onClick = {
                    if (calendarMonth == 11) {
                        calendarMonth = 0
                        calendarYear += 1
                    } else {
                        calendarMonth += 1
                    }
                    selectedDay = 1
                }, modifier = Modifier.testTag("next_month")) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Calendar Grid Header (Weeks label)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grid contents
        var dayCounter = 1
        val weeks = mutableListOf<List<Int?>>()
        var currentWeek = mutableListOf<Int?>()

        // Fill starting empty slot placeholders
        for (i in 0 until emptyDaysBefore) {
            currentWeek.add(null)
        }

        while (dayCounter <= maxDays) {
            currentWeek.add(dayCounter)
            dayCounter++
            if (currentWeek.size == 7) {
                weeks.add(currentWeek)
                currentWeek = mutableListOf()
            }
        }

        if (currentWeek.isNotEmpty()) {
            while (currentWeek.size < 7) {
                currentWeek.add(null)
            }
            weeks.add(currentWeek)
        }

        // Render standard calendar grid
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dayNum ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (dayNum != null && selectedDay == dayNum)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Transparent
                                )
                                .border(
                                    width = if (dayNum != null && isToday(dayNum, calendarMonth, calendarYear)) 1.2.dp else 0.dp,
                                    color = if (dayNum != null && isToday(dayNum, calendarMonth, calendarYear)) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = dayNum != null) {
                                    selectedDay = dayNum
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = dayNum.toString(),
                                        fontWeight = if (selectedDay == dayNum) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (selectedDay == dayNum)
                                            Color.White
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )

                                    // Compute indicator dots for activities of that day
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val dayActivities = getDayActivityCounts(dayNum, calendarMonth, calendarYear, expenses, bills, checkups, medicines)
                                        if (dayActivities.hasGrocery && (calendarFilterType == "All" || calendarFilterType == "Grocery")) {
                                            DotBadge(Color(0xFF4caf50))
                                        }
                                        if (dayActivities.hasUtility && (calendarFilterType == "All" || calendarFilterType == "Utility")) {
                                            DotBadge(Color(0xFF2196f3))
                                        }
                                        if (dayActivities.hasHealth && (calendarFilterType == "All" || calendarFilterType == "Health")) {
                                            DotBadge(Color(0xFFf44336))
                                        }
                                        if (dayActivities.hasClothes && (calendarFilterType == "All" || calendarFilterType == "Clothes")) {
                                            DotBadge(Color(0xFF9c27b0))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Category selector of Dot badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("All", "Grocery", "Utility", "Health", "Clothes").forEach { itemType ->
                FilterChip(
                    selected = calendarFilterType == itemType,
                    onClick = { calendarFilterType = itemType },
                    label = { Text(itemType, fontSize = 10.sp) },
                    modifier = Modifier.height(26.dp).testTag("cal_filter_$itemType")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Day agenda header text
        selectedDay?.let { day ->
            Text(
                titleForDay(day, calendarMonth, calendarYear, calendarFilterType),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Load all relevant filtered lists match this day
            val dayActivities = remember(day, calendarMonth, calendarYear, expenses, bills, checkups, medicines, calendarFilterType) {
                getDaySpecificAgenda(day, calendarMonth, calendarYear, expenses, bills, checkups, medicines, calendarFilterType)
            }

            if (dayActivities.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No purchases or bills due on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayActivities) { agenda ->
                        AgendaItemCard(agenda, viewModel)
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DotBadge(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun AgendaItemCard(item: AgendaEntity, viewModel: HouseholdViewModel) {
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()
    val indicatorColor = when (item.type) {
        "Grocery" -> Color(0xFF4caf50)
        "Utility" -> Color(0xFF2196f3)
        "Health" -> Color(0xFFf44336)
        "Clothes" -> Color(0xFF9c27b0)
        else -> Color(0xFF00bcd4)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }

            Text(
                text = "$symbol ${item.cost.toInt()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun isToday(dayNum: Int, monthIdx: Int, yearVal: Int): Boolean {
    val today = Calendar.getInstance()
    return today.get(Calendar.DAY_OF_MONTH) == dayNum &&
            today.get(Calendar.MONTH) == monthIdx &&
            today.get(Calendar.YEAR) == yearVal
}

fun titleForDay(day: Int, monthIdx: Int, yearVal: Int, filter: String): String {
    val label = if (filter == "All") "Activities on" else "$filter purchases on"
    return "$label ${getMonthName(monthIdx + 1)} $day, $yearVal"
}

// Helpers for calendar indicator computations
data class DayActivityCounts(
    val hasGrocery: Boolean = false,
    val hasUtility: Boolean = false,
    val hasHealth: Boolean = false,
    val hasClothes: Boolean = false
)

fun getDayActivityCounts(
    day: Int, monthIdx: Int, yearVal: Int,
    expenses: List<com.example.data.ExpenseRecord>,
    bills: List<com.example.data.UtilityBill>,
    checkups: List<com.example.data.HealthCheckup>,
    medicines: List<com.example.data.MedicinePurchase>
): DayActivityCounts {
    val cal = Calendar.getInstance()

    val grocery = expenses.any {
        cal.timeInMillis = it.purchaseDate
        cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal && it.category == "Grocery"
    }
    val clothes = expenses.any {
        cal.timeInMillis = it.purchaseDate
        cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal && (it.category == "Clothes" || it.category == "Slippers" || it.category == "Personal")
    }
    val utility = bills.any {
        cal.timeInMillis = it.dueDate
        cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal
    }
    val health = checkups.any {
        cal.timeInMillis = it.date
        cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal
    } || medicines.any {
        cal.timeInMillis = it.purchaseDate
        cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal
    }

    return DayActivityCounts(grocery, utility, health, clothes)
}

data class AgendaEntity(
    val title: String,
    val subtitle: String,
    val cost: Double,
    val type: String // "Grocery", "Utility", "Health", "Clothes"
)

fun getDaySpecificAgenda(
    day: Int, monthIdx: Int, yearVal: Int,
    expenses: List<com.example.data.ExpenseRecord>,
    bills: List<com.example.data.UtilityBill>,
    checkups: List<com.example.data.HealthCheckup>,
    medicines: List<com.example.data.MedicinePurchase>,
    filter: String
): List<AgendaEntity> {
    val list = mutableListOf<AgendaEntity>()
    val cal = Calendar.getInstance()

    // 1. Matches expenses
    expenses.forEach { exp ->
        cal.timeInMillis = exp.purchaseDate
        if (cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal) {
            val type = if (exp.category == "Grocery") "Grocery" else "Clothes"
            if (filter == "All" || filter == type) {
                list.add(
                    AgendaEntity(
                        title = exp.itemName,
                        subtitle = "${exp.category} bought for ${exp.purchasedForName}",
                        cost = exp.totalAmount,
                        type = type
                    )
                )
            }
        }
    }

    // 2. Matches bills
    bills.forEach { b ->
        cal.timeInMillis = b.dueDate
        if (cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal) {
            if (filter == "All" || filter == "Utility") {
                list.add(
                    AgendaEntity(
                        title = "${b.billType} utility bill due",
                        subtitle = "Status: ${b.status}",
                        cost = b.amount,
                        type = "Utility"
                    )
                )
            }
        }
    }

    // 3. Matches checkups
    checkups.forEach { c ->
        cal.timeInMillis = c.date
        if (cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal) {
            if (filter == "All" || filter == "Health") {
                list.add(
                    AgendaEntity(
                        title = "Clinic Visit: ${c.patientName}",
                        subtitle = "Diagnosis: ${c.diagnosis} at ${c.hospitalOrClinic}",
                        cost = c.totalCost,
                        type = "Health"
                    )
                )
            }
        }
    }

    // 4. Matches medicine
    medicines.forEach { m ->
        cal.timeInMillis = m.purchaseDate
        if (cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == monthIdx && cal.get(Calendar.YEAR) == yearVal) {
            if (filter == "All" || filter == "Health") {
                list.add(
                    AgendaEntity(
                        title = "Med: ${m.medicineName}",
                        subtitle = "For ${m.patientName} (${m.purpose})",
                        cost = m.totalAmount,
                        type = "Health"
                    )
                )
            }
        }
    }

    return list
}
