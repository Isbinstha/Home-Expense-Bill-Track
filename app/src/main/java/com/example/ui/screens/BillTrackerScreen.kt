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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UtilityBill
import com.example.data.RecurringBillTemplate
import com.example.ui.viewmodel.HouseholdViewModel
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillTrackerScreen(
    viewModel: HouseholdViewModel,
    modifier: Modifier = Modifier
) {
    val bills by viewModel.utilityBills.collectAsState()
    val showAddDialog by viewModel.showAddBillDialog.collectAsState()

    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()
    val templates by viewModel.templates.collectAsState()

    var showTemplatesSection by remember { mutableStateOf(false) }
    var showAddTemplateDialog by remember { mutableStateOf(false) }

    var statusFilter by remember { mutableStateOf("All") } // "All", "Unpaid", "Paid"

    // Hide Pending Confirmation bills from the normal listing; they are highlighted in the prompt banner
    val filteredBills = bills.filter { bill ->
        bill.status != "Pending Confirmation" && when (statusFilter) {
            "Paid" -> bill.status == "Paid"
            "Unpaid" -> bill.status == "Unpaid"
            else -> true
        }
    }

    val totalUnpaid = bills.filter { it.status == "Unpaid" }.sumOf { it.amount }
    val totalPaid = bills.filter { it.status == "Paid" }.sumOf { it.amount }

    if (showAddTemplateDialog) {
        AddTemplateDialog(viewModel = viewModel, onDismiss = { showAddTemplateDialog = false })
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Summary metrics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Unpaid", style = MaterialTheme.typography.labelMedium)
                        Text("$symbol ${totalUnpaid.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.height(30.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Paid This Period", style = MaterialTheme.typography.labelMedium)
                        Text("$symbol ${totalPaid.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF2ec4b6))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recurring Bill Templates setup and overview panel
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, contentDescription = "Templates", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Utility Bill Templates", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        TextButton(onClick = { showTemplatesSection = !showTemplatesSection }) {
                            Text(if (showTemplatesSection) "Hide Drawer" else "Manage (${templates.size})", fontSize = 12.sp)
                        }
                    }

                    if (showTemplatesSection) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showAddTemplateDialog = true },
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("btn_setup_template"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("+ Setup New Template", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (templates.isEmpty()) {
                            Text("No recurring bill templates set up yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            templates.forEach { temp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${temp.billType} Template", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "Due Day: ${temp.dueDayOfMonth}th • Base: $symbol${temp.defaultAmount.toInt()} ${if (temp.usePreviousAmount) "• Auto-adjusts" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeTemplate(temp) }, modifier = Modifier.size(28.dp).testTag("delete_template_${temp.id}")) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Template", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending confirmations section for generated bills
            val pendingConfirmationBills = bills.filter { it.status == "Pending Confirmation" }

            if (pendingConfirmationBills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationImportant, contentDescription = "Confirmation Required", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pending Confirmations (${pendingConfirmationBills.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text("Verify monthly schedule entries pre-filled from recurring templates:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f))
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        pendingConfirmationBills.forEach { bill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${bill.billType} - ${viewModel.getMonthName(bill.billingMonth)} ${bill.billingYear}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Estimated: $symbol${bill.amount.toInt()} • Due ${viewModel.formatDate(bill.dueDate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            val confirmed = bill.copy(status = "Unpaid")
                                            viewModel.addUtilityBill(confirmed)
                                            viewModel.fireLocalNotification("Bill Confirmed", "Confirmed $symbol${bill.amount.toInt()} ${bill.billType} bill.")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("confirm_bill_${bill.id}")
                                    ) {
                                        Text("Confirm Due", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val paid = bill.copy(status = "Paid", paymentDate = System.currentTimeMillis())
                                            viewModel.addUtilityBill(paid)
                                            viewModel.fireLocalNotification("Bill Confirmed & Paid", "Paid $symbol${bill.amount.toInt()} ${bill.billType} bill.")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ec4b6)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("pay_confirm_bill_${bill.id}")
                                    ) {
                                        Text("Pay", fontSize = 10.sp)
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeUtilityBill(bill) },
                                        modifier = Modifier.size(28.dp).testTag("delete_pending_bill_${bill.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All", "Unpaid", "Paid").forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { Text(status, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("bill_filter_$status")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredBills.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No utility bills tracked.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredBills, key = { it.id }) { bill ->
                        BillRowItem(bill = bill, viewModel = viewModel)
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Bill FAB
        FloatingActionButton(
            onClick = { viewModel.showAddBillDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_bill_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Bill")
        }

        if (showAddDialog) {
            AddBillDialog(viewModel = viewModel, onDismiss = { viewModel.showAddBillDialog.value = false })
        }
    }
}

@Composable
fun BillRowItem(bill: UtilityBill, viewModel: HouseholdViewModel) {
    val isPaid = bill.status == "Paid"
    val themeColor = if (isPaid) Color(0xFF2ec4b6) else Color(0xFFe71d36)
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bill_row_${bill.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(getBillTypeColor(bill.billType).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getBillTypeIcon(bill.billType),
                            contentDescription = null,
                            tint = getBillTypeColor(bill.billType),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${bill.billType} Bill",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Billing Period: ${getMonthName(bill.billingMonth)} ${bill.billingYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$symbol ${bill.amount.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = bill.status,
                            color = themeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
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
                Column {
                    Text(
                        text = "Due Date: ${viewModel.formatDate(bill.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFC62828),
                        fontWeight = if (isPaid) FontWeight.Normal else FontWeight.Bold
                    )
                    bill.paymentDate?.let { pDate ->
                        Text(
                            text = "Paid On: ${viewModel.formatDate(pDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00796B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isPaid) {
                        Button(
                            onClick = {
                                // Mark paid
                                val updated = bill.copy(status = "Paid", paymentDate = System.currentTimeMillis())
                                viewModel.addUtilityBill(updated) // Triggers replace update in Dao
                                viewModel.fireLocalNotification("Bill Paid", "Marked ${bill.billType} bill of $symbol ${bill.amount.toInt()} as Paid successfully.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ec4b6)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).testTag("pay_bill_btn_${bill.id}")
                        ) {
                            Text("Mark Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.removeUtilityBill(bill) },
                        modifier = Modifier.size(30.dp).testTag("delete_bill_${bill.id}")
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

            // Specs panel if electricity/water/gas details are entered
            if (bill.unitsConsumed > 0.0 || bill.referenceNumber.isNotEmpty() || bill.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (bill.unitsConsumed > 0.0) {
                            Text(
                                "Consumption: ${bill.unitsConsumed} ${if (bill.billType == "Water") "kL" else "kWh"}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (bill.referenceNumber.isNotEmpty()) {
                            Text("Ref. Number: ${bill.referenceNumber}", style = MaterialTheme.typography.labelSmall)
                        }
                        if (bill.notes.isNotEmpty()) {
                            Text("Notes: ${bill.notes}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddBillDialog(
    viewModel: HouseholdViewModel,
    onDismiss: () -> Unit
) {
    val symbol = viewModel.getCurrencySymbol()
    var billType by remember { mutableStateOf("Electricity") }
    var billingMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var billingYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var amountStr by remember { mutableStateOf("") }
    var dueDateMs by remember { mutableStateOf(System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000)) } // 3 days in future default
    var unitsConsumedStr by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Utility Bill", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bill Type Horizontal choice
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bill Type: ", fontWeight = FontWeight.Bold)
                    var typeDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { typeDropdown = true }) {
                            Text(billType)
                        }
                        DropdownMenu(expanded = typeDropdown, onDismissRequest = { typeDropdown = false }) {
                            listOf("Electricity", "Water", "Gas Cylinder", "Internet", "TV", "Mobile").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        billType = type
                                        typeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Bill Amount ($symbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_bill_amount")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Month & Year selects
                    var monthDrop by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { monthDrop = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Month: $billingMonth")
                        }
                        DropdownMenu(expanded = monthDrop, onDismissRequest = { monthDrop = false }) {
                            (1..12).forEach { mIdx ->
                                DropdownMenuItem(text = { Text("$mIdx (${getMonthName(mIdx)})") }, onClick = {
                                    billingMonth = mIdx
                                    monthDrop = false
                                })
                            }
                        }
                    }

                    var yearDrop by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { yearDrop = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Year: $billingYear")
                        }
                        DropdownMenu(expanded = yearDrop, onDismissRequest = { yearDrop = false }) {
                            (2025..2030).forEach { yr ->
                                DropdownMenuItem(text = { Text("$yr") }, onClick = {
                                    billingYear = yr
                                    yearDrop = false
                                })
                            }
                        }
                    }
                }

                // Due Date simulator controls (simulate picker)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Due date option: ", fontWeight = FontWeight.Bold)
                    var dateMenu by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { dateMenu = true }) {
                            Text(viewModel.formatDate(dueDateMs))
                        }
                        DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                            DropdownMenuItem(text = { Text("Today") }, onClick = { dueDateMs = System.currentTimeMillis(); dateMenu = false })
                            DropdownMenuItem(text = { Text("In 3 Days") }, onClick = { dueDateMs = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000); dateMenu = false })
                            DropdownMenuItem(text = { Text("In 7 Days") }, onClick = { dueDateMs = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000); dateMenu = false })
                            DropdownMenuItem(text = { Text("In 15 Days") }, onClick = { dueDateMs = System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000); dateMenu = false })
                        }
                    }
                }

                if (billType == "Electricity" || billType == "Water") {
                    OutlinedTextField(
                        value = unitsConsumedStr,
                        onValueChange = { unitsConsumedStr = it },
                        label = { Text("Units Consumed (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Reference / ID Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Service Provider") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text("Please input a valid positive amount.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        val units = unitsConsumedStr.toDoubleOrNull() ?: 0.0
                        val bill = UtilityBill(
                            billType = billType,
                            billingMonth = billingMonth,
                            billingYear = billingYear,
                            amount = amt,
                            dueDate = dueDateMs,
                            status = "Unpaid",
                            unitsConsumed = units,
                            referenceNumber = referenceNumber.trim(),
                            notes = notes.trim()
                        )
                        viewModel.addUtilityBill(bill)
                        viewModel.fireLocalNotification("Bill Created", "Stored new pending ${billType} bill of $symbol ${amt.toInt()}.")
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("submit_bill")
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

fun getBillTypeIcon(billType: String): ImageVector {
    return when (billType) {
        "Electricity" -> Icons.Default.OfflineBolt
        "Water" -> Icons.Default.WaterDrop
        "Gas Cylinder" -> Icons.Default.LocalGasStation
        "Internet" -> Icons.Default.Wifi
        "TV" -> Icons.Default.Tv
        else -> Icons.Default.Smartphone
    }
}

fun getBillTypeColor(billType: String): Color {
    return when (billType) {
        "Electricity" -> Color(0xFFffb703)
        "Water" -> Color(0xFF2196f3)
        "Gas Cylinder" -> Color(0xFFf44336)
        "Internet" -> Color(0xFF9c27b0)
        "TV" -> Color(0xFF607d8b)
        else -> Color(0xFF4caf50)
    }
}

@Composable
fun AddTemplateDialog(
    viewModel: HouseholdViewModel,
    onDismiss: () -> Unit
) {
    var billType by remember { mutableStateOf("Electricity") }
    var defaultAmountStr by remember { mutableStateOf("") }
    var dueDayOfMonthStr by remember { mutableStateOf("15") }
    var usePreviousAmount by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }

    val symbol = viewModel.getCurrencySymbol()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Up Recurring Template", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Automatically pre-generates monthly bills for you based on this template.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Bill Type: ", fontWeight = FontWeight.Bold)
                    var typeDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { typeDropdown = true }) {
                            Text(billType)
                        }
                        DropdownMenu(expanded = typeDropdown, onDismissRequest = { typeDropdown = false }) {
                            listOf("Electricity", "Water", "Gas Cylinder", "Internet", "TV", "Mobile").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        billType = type
                                        typeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = defaultAmountStr,
                    onValueChange = { defaultAmountStr = it },
                    label = { Text("Base Amount ($symbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_template_amount")
                )

                OutlinedTextField(
                    value = dueDayOfMonthStr,
                    onValueChange = { dueDayOfMonthStr = it },
                    label = { Text("Expected Due Day of Month (1-28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_template_day")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dynamic Auto-Adjust", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text("Base price on the most recent previous paid bill amount if available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = usePreviousAmount,
                        onCheckedChange = { usePreviousAmount = it },
                        modifier = Modifier.testTag("template_auto_adjust")
                    )
                }

                if (showError) {
                    Text("Please enter a valid amount and day of month.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = defaultAmountStr.toDoubleOrNull()
                    val day = dueDayOfMonthStr.toIntOrNull()
                    if (amt != null && amt > 0 && day != null && day in 1..28) {
                        val temp = RecurringBillTemplate(
                            billType = billType,
                            defaultAmount = amt,
                            dueDayOfMonth = day,
                            usePreviousAmount = usePreviousAmount,
                            notes = "Active recurring check"
                        )
                        viewModel.addTemplate(temp)
                        viewModel.fireLocalNotification("Template Setup", "Recurring template for $billType registered successfully.")
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("submit_template")
            ) {
                Text("Set Up Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
