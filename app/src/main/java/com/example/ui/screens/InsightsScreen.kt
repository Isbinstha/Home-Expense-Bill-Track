package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseRecord
import com.example.ui.viewmodel.HouseholdViewModel
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    viewModel: HouseholdViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenseRecords.collectAsState()
    val bills by viewModel.utilityBills.collectAsState()
    val checkups by viewModel.healthCheckups.collectAsState()
    val meds by viewModel.medicinePurchases.collectAsState()

    var activeTrendType by remember { mutableStateOf("Category") } // "Category", "Utility", "History"

    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))

        // Insight header text
        Text(
            "Productivity & Spending Insights",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Elegantly tracking inflation, utility drift, and category limits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Inner selector horizontal bar
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Category", "Utility", "History").forEach { tab ->
                val isSelected = activeTrendType == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTrendType = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (activeTrendType) {
            "Category" -> CategoryInsightTab(expenses, symbol)
            "Utility" -> UtilityInsightTab(bills, symbol)
            else -> InflationHistoryTab(expenses, viewModel, symbol)
        }
    }
}

@Composable
fun CategoryInsightTab(expenses: List<ExpenseRecord>, symbol: String) {
    // Group categories in current calendar period
    val calendar = Calendar.getInstance()
    val curMonth = calendar.get(Calendar.MONTH) + 1
    val curYear = calendar.get(Calendar.YEAR)

    val monthExp = expenses.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.purchaseDate }
        c.get(Calendar.MONTH) + 1 == curMonth && c.get(Calendar.YEAR) == curYear
    }

    val totalsByCategory = monthExp.groupBy { it.category }.mapValues { entry ->
        entry.value.sumOf { it.totalAmount }
    }

    val grandTotal = totalsByCategory.values.sum()

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Spending Distribution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total recorded this month: $symbol ${grandTotal.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (grandTotal == 0.0) {
                        EmptyStateInsight("Log items in 'Expenses' tab to render visual budget graphs.")
                    } else {
                        // Custom drawn proportional stacked bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            totalsByCategory.forEach { (category, sum) ->
                                val pct = sum / grandTotal
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight((pct * 100).toFloat().coerceAtLeast(1f))
                                        .background(getCategoryColor(category))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category rows with animated meters
                        totalsByCategory.toList().sortedByDescending { it.second }.forEach { (category, sum) ->
                            val pct = (sum / grandTotal).toFloat()
                            val animatedProgress by animateFloatAsState(
                                targetValue = pct,
                                animationSpec = tween(durationMillis = 800)
                            )

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(category))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text(
                                        "$symbol ${sum.toInt()} (${(pct * 100).toInt()}%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = animatedProgress,
                                    color = getCategoryColor(category),
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UtilityInsightTab(bills: List<com.example.data.UtilityBill>, symbol: String) {
    // Month name bar trends
    val billGroup = bills.groupBy { getMonthName(it.billingMonth) }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Utility Bills Bill Drifts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Month-wise utility expenditure trends (Electricity/Water/Gas)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(20.dp))

                    if (billGroup.isEmpty()) {
                        EmptyStateInsight("Log utility bills in the 'Utility' tab to render history meters.")
                    } else {
                        val maxBill = billGroup.values.maxOrNull() ?: 1.0

                        // Render lightweight, beautifully aligned vertical bars using standard Compose Rows!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            billGroup.toList().take(6).forEach { (monthName, totalSpent) ->
                                val pct = (totalSpent / maxBill).toFloat().coerceIn(0.05f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "$symbol${totalSpent.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(pct)
                                            .width(24.dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        monthName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InflationHistoryTab(expenses: List<com.example.data.ExpenseRecord>, viewModel: HouseholdViewModel, symbol: String) {
    val itemsUniqueList = expenses.map { it.itemName }.distinct().sorted()
    var selectedItemQuery by remember { mutableStateOf("") }
    var matchesListExpanded by remember { mutableStateOf(false) }

    val matchOptions = itemsUniqueList.filter { it.contains(selectedItemQuery, ignoreCase = true) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Grocery & Item Inflation Lookups", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Select a household item to see historical price changes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated Search Auto-complete TextField
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedItemQuery,
                            onValueChange = {
                                selectedItemQuery = it
                                matchesListExpanded = true
                            },
                            placeholder = { Text("Search product name (Rice, Milk, Soap...)") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("inflation_search_box"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (matchesListExpanded && selectedItemQuery.isNotEmpty()) {
                            DropdownMenu(
                                expanded = matchesListExpanded,
                                onDismissRequest = { matchesListExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                matchOptions.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            selectedItemQuery = name
                                            matchesListExpanded = false
                                        }
                                    )
                                }
                                if (matchOptions.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No matching products found") }, onClick = {})
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val itemPurchases = expenses.filter { it.itemName.equals(selectedItemQuery, ignoreCase = true) }

                    if (itemPurchases.isEmpty()) {
                        EmptyStateInsight("Search or select an item above to display the drift log.")
                    } else {
                        val rates = itemPurchases.map { it.rate }
                        val highestRate = rates.maxOrNull() ?: 0.0
                        val lowestRate = rates.minOrNull() ?: 0.0
                        val avRate = rates.average()

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tracking Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Total purchased: ${itemPurchases.size} times", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Avg price: $symbol ${avRate.toInt()}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // High vs Low meters
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Highest Rate", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                                    Text("$symbol ${highestRate.toInt()}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828), fontSize = 18.sp)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Lowest Rate", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                    Text("$symbol ${lowestRate.toInt()}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32), fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Historical purchases timeline:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(6.dp))

                        itemPurchases.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(viewModel.formatDate(log.purchaseDate), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Qty: ${log.quantity} ${log.unit} • For: ${log.purchasedForName}", style = MaterialTheme.typography.labelSmall)
                                }
                                Text("$symbol ${log.rate.toInt()}/${log.unit}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateInsight(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
