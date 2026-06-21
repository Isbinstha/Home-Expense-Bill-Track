package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseRecord
import com.example.data.FamilyMember
import com.example.ui.viewmodel.HouseholdViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(
    viewModel: HouseholdViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenseRecords.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val showAddDialog by viewModel.showAddExpenseDialog.collectAsState()

    val searchQuery by viewModel.expenseSearchQuery.collectAsState()
    val catFilter by viewModel.expenseCategoryFilter.collectAsState()
    val famFilter by viewModel.expenseFamilyFilter.collectAsState()

    val selectedPriceHistoryItem by viewModel.selectedPriceHistoryItem.collectAsState()

    // Filter logic
    val filteredExpenses = expenses.filter { exp ->
        val matchesSearch = exp.itemName.contains(searchQuery, ignoreCase = true) ||
                exp.notes.contains(searchQuery, ignoreCase = true)
        val matchesCat = catFilter == null || exp.category == catFilter
        val matchesFam = famFilter == null || exp.purchasedForName == famFilter
        matchesSearch && matchesCat && matchesFam
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Summary Banner
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.expenseSearchQuery.value = it },
                placeholder = { Text("Search items (rice, slippers, uniforms...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.expenseSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("expense_search_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filters horizontal row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Filter Menu
                var showCatMenu by remember { mutableStateOf(false) }
                Button(
                    onClick = { showCatMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (catFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (catFilter != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("filter_cat_button")
                ) {
                    Text(text = catFilter ?: "Category", fontSize = 12.sp, maxLines = 1)
                    DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                viewModel.expenseCategoryFilter.value = null
                                showCatMenu = false
                            }
                        )
                        listOf("Grocery", "Clothes", "Slippers", "Personal", "Other").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    viewModel.expenseCategoryFilter.value = cat
                                    showCatMenu = false
                                }
                            )
                        }
                    }
                }

                // Family member Filter Menu
                var showFamMenu by remember { mutableStateOf(false) }
                Button(
                    onClick = { showFamMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (famFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (famFilter != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("filter_family_button")
                ) {
                    Text(text = famFilter ?: "Paid For", fontSize = 12.sp, maxLines = 1)
                    DropdownMenu(expanded = showFamMenu, onDismissRequest = { showFamMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Everyone") },
                            onClick = {
                                viewModel.expenseFamilyFilter.value = null
                                showFamMenu = false
                            }
                        )
                        (listOf("Everyone", "Household") + familyMembers.map { it.name }).forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member) },
                                onClick = {
                                    viewModel.expenseFamilyFilter.value = member
                                    showFamMenu = false
                                }
                            )
                        }
                    }
                }

                if (catFilter != null || famFilter != null) {
                    IconButton(
                        onClick = {
                            viewModel.expenseCategoryFilter.value = null
                            viewModel.expenseFamilyFilter.value = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FilterListOff, contentDescription = "Clear Filters")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main list of items
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ProductionQuantityLimits,
                            contentDescription = "",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No expenses match your filters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            "Tap the '+' FAB below to register purchases.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        ExpenseListRow(
                            expense = expense,
                            viewModel = viewModel,
                            onItemClick = {
                                viewModel.selectedPriceHistoryItem.value = expense.itemName
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.showAddExpenseDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_expense_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Grocery / Item")
        }

        // Dialog for adding new expense
        if (showAddDialog) {
            AddExpenseDialog(
                viewModel = viewModel,
                familyMembers = familyMembers,
                onDismiss = { viewModel.showAddExpenseDialog.value = false }
            )
        }

        // Collapsible Bottom Sheet price history comparison widget
        if (selectedPriceHistoryItem.isNotEmpty()) {
            PriceHistoryModal(
                itemName = selectedPriceHistoryItem,
                viewModel = viewModel,
                onDismiss = { viewModel.selectedPriceHistoryItem.value = "" }
            )
        }
    }
}

@Composable
fun ExpenseListRow(
    expense: ExpenseRecord,
    viewModel: HouseholdViewModel,
    onItemClick: () -> Unit
) {
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("expense_row_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(expense.category).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(expense.category),
                            contentDescription = null,
                            tint = getCategoryColor(expense.category),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = expense.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${expense.category} | ${expense.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$symbol ${expense.totalAmount.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { viewModel.removeExpenseRecord(expense) },
                        modifier = Modifier.size(24.dp).testTag("delete_expense_${expense.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "For: ${expense.purchasedForName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${expense.quantity} ${expense.unit} x $symbol${expense.rate.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = viewModel.formatDate(expense.purchaseDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (expense.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${expense.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    viewModel: HouseholdViewModel,
    familyMembers: List<FamilyMember>,
    onDismiss: () -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Grocery") }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var rateStr by remember { mutableStateOf("") }
    var purchasedFor by remember { mutableStateOf("Everyone") }
    var vendorName by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New Purchase", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("Rice, Slippers, Shoes, Uniform...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_item_name"),
                    singleLine = true
                )

                // Category options Select
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Category: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    var showCatDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { showCatDropdown = true }) {
                            Text(category)
                        }
                        DropdownMenu(expanded = showCatDropdown, onDismissRequest = { showCatDropdown = false }) {
                            listOf("Grocery", "Clothes", "Slippers", "Personal", "Other").forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        showCatDropdown = false
                                        // Auto adjust standard units
                                        unit = when(cat) {
                                            "Grocery" -> "kg"
                                            "Clothes", "Slippers" -> "pcs"
                                            else -> "pcs"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_quantity"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg, packet...)") },
                        modifier = Modifier.weight(1.5f).testTag("input_unit"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Price per Unit (Rate)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_rate"),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("For Member: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    var showForDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { showForDropdown = true }) {
                            Text(purchasedFor)
                        }
                        DropdownMenu(expanded = showForDropdown, onDismissRequest = { showForDropdown = false }) {
                            (listOf("Everyone", "Household") + familyMembers.map { it.name }).forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member) },
                                    onClick = {
                                        purchasedFor = member
                                        showForDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = vendorName,
                    onValueChange = { vendorName = it },
                    label = { Text("Shop / Vendor (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paid via: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf("Cash", "Online", "Credit").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                if (showError) {
                    Text("Please fill out Name, Qty and Rate correctly.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantityStr.toDoubleOrNull()
                    val r = rateStr.toDoubleOrNull()
                    if (itemName.isNotBlank() && q != null && r != null) {
                        val record = ExpenseRecord(
                            category = category,
                            itemName = itemName.trim(),
                            quantity = q,
                            unit = unit.trim(),
                            rate = r,
                            totalAmount = q * r,
                            purchasedForName = purchasedFor,
                            purchaseDate = System.currentTimeMillis(),
                            vendorName = vendorName.trim(),
                            paymentMethod = paymentMethod,
                            notes = notes.trim()
                        )
                        viewModel.addExpenseRecord(record)
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("submit_expense")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PriceHistoryModal(
    itemName: String,
    viewModel: HouseholdViewModel,
    onDismiss: () -> Unit
) {
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()
    val expenses by viewModel.expenseRecords.collectAsState()
    val itemLogs = expenses.filter { it.itemName.equals(itemName, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Price History of \"$itemName\"", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(text = "Tracked rates & inflation patterns over time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        },
        text = {
            if (itemLogs.isEmpty()) {
                Text("No data available.")
            } else {
                val rates = itemLogs.map { it.rate }
                val maxPrice = rates.maxOrNull() ?: 0.0
                val minPrice = rates.minOrNull() ?: 0.0
                val avgPrice = rates.average()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Highlights panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Average Rate", style = MaterialTheme.typography.labelSmall)
                                Text("$symbol ${avgPrice.toInt()}/u", fontWeight = FontWeight.ExtraBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Highest", style = MaterialTheme.typography.labelSmall)
                                Text("$symbol ${maxPrice.toInt()}/u", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Lowest", style = MaterialTheme.typography.labelSmall)
                                Text("$symbol ${minPrice.toInt()}/u", fontWeight = FontWeight.Bold, color = Color(0xFF2ec4b6))
                            }
                        }
                    }

                    Text("Purchase Timeline:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(itemLogs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = viewModel.formatDate(log.purchaseDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Qty: ${log.quantity} ${log.unit} • For: ${log.purchasedForName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$symbol ${log.rate.toInt()}/${log.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Total Spent: $symbol ${log.totalAmount.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}
