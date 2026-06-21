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
import com.example.data.Budget
import com.example.data.FamilyMember
import com.example.ui.viewmodel.HouseholdViewModel
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: HouseholdViewModel,
    modifier: Modifier = Modifier
) {
    val familyMembers by viewModel.familyMembers.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val lockSettings by viewModel.lockSettings.collectAsState()

    val showMemberDialog by viewModel.showAddMemberDialog.collectAsState()
    val showBudgetDialog by viewModel.showAddBudgetDialog.collectAsState()

    val driveConnected by viewModel.googleDriveConnected.collectAsState()
    val lastBackupT by viewModel.lastBackupTimestamp.collectAsState()
    val autoBackup by viewModel.autoBackupEnabled.collectAsState()

    var activeSubSettingTab by remember { mutableStateOf("Profile") } // "Profile", "Budgets", "Security"

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))

        // Navigation tab row
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Profile", "Budgets", "Security").forEach { tab ->
                val isSelected = activeSubSettingTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeSubSettingTab = tab }
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

        when (activeSubSettingTab) {
            "Profile" -> ProfileSettingsTab(
                viewModel = viewModel,
                members = familyMembers,
                showAddMember = { viewModel.showAddMemberDialog.value = true },
                driveConnected = driveConnected,
                lastBackupT = lastBackupT,
                autoBackup = autoBackup
            )
            "Budgets" -> BudgetsTab(
                viewModel = viewModel,
                budgets = budgets,
                showAddBudget = { viewModel.showAddBudgetDialog.value = true }
            )
            else -> SecurityLockTab(viewModel, lockSettings)
        }

        // Add Member Dialog
        if (showMemberDialog) {
            AddMemberDialog(viewModel = viewModel, onDismiss = { viewModel.showAddMemberDialog.value = false })
        }

        // Add Budget Dialog
        if (showBudgetDialog) {
            AddBudgetDialog(viewModel = viewModel, onDismiss = { viewModel.showAddBudgetDialog.value = false })
        }
    }
}

@Composable
fun ProfileSettingsTab(
    viewModel: HouseholdViewModel,
    members: List<FamilyMember>,
    showAddMember: () -> Unit,
    driveConnected: Boolean,
    lastBackupT: String?,
    autoBackup: Boolean
) {
    val activeProfile by viewModel.currentProfileName.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // App Profile Customizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Active Session Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Change profile context for logging and reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    var showProfilesDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { showProfilesDropdown = true }) {
                            Text("Active: $activeProfile")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = showProfilesDropdown, onDismissRequest = { showProfilesDropdown = false }) {
                            listOf("Primary Owner", "Father", "Mother", "Son", "Daughter").forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.currentProfileName.value = name
                                        showProfilesDropdown = false
                                        viewModel.fireLocalNotification("Profile Changed", "Active profile context switched to $name.")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Family Members List config
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Household Members", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = showAddMember, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(30.dp).testTag("add_member_btn")) {
                            Text("+ Add Member", fontSize = 11.sp)
                        }
                    }
                    Text("Register family members to tag medicine, clothing & items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (members.isEmpty()) {
                        Text("No other family members registered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        members.forEach { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(m.badgeColorHex)))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${m.name} (${m.relation})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                IconButton(onClick = { viewModel.removeFamilyMember(m) }, modifier = Modifier.size(24.dp).testTag("delete_member_${m.id}")) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Google Drive Sync Mock section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Secure Google Drive Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Encrypt and compress database records to personal Drive profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (driveConnected) "Connected to: isbinshrestha@gmail.com" else "Profile Disconnected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (driveConnected) Color(0xFF2ec4b6) else MaterialTheme.colorScheme.error
                        )
                        Switch(
                            checked = driveConnected,
                            onCheckedChange = {
                                viewModel.googleDriveConnected.value = it
                                if (it) {
                                    viewModel.fireLocalNotification("Drive Configured", "Authorizing encrypted container folder successfully.")
                                }
                            },
                            modifier = Modifier.testTag("drive_toggle_switch")
                        )
                    }

                    if (driveConnected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Last synced size backup:", style = MaterialTheme.typography.labelSmall)
                                Text(lastBackupT ?: "Never", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(onClick = { viewModel.triggerGoogleDriveBackup() }, modifier = Modifier.testTag("backup_now_btn")) {
                                Text("Backup Now")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto sync backup (Daily)", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = autoBackup,
                                onCheckedChange = { viewModel.autoBackupEnabled.value = it },
                                modifier = Modifier.testTag("auto_backup_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsTab(
    viewModel: HouseholdViewModel,
    budgets: List<Budget>,
    showAddBudget: () -> Unit
) {
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category Monthly Limits", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = showAddBudget, modifier = Modifier.testTag("add_budget_btn")) {
                            Text("+ Add Limit")
                        }
                    }
                    Text("Trigger warning indicators at 80% and flashing alerts at 100%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (budgets.isEmpty()) {
                        Text("No budget limits declared yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        budgets.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = b.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Period: ${getMonthName(b.month)} ${b.year}", style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(text = "$symbol ${b.budgetLimit.toInt()}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = { viewModel.removeBudget(b) }, modifier = Modifier.size(24.dp).testTag("delete_budget_${b.id}")) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
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
fun SecurityLockTab(
    viewModel: HouseholdViewModel,
    settings: com.example.data.AppLockSettings?
) {
    var pinCode by remember { mutableStateOf(settings?.pinCode ?: "") }
    var securityQ by remember { mutableStateOf(settings?.securityQuestion ?: "") }
    var securityA by remember { mutableStateOf(settings?.securityAnswer ?: "") }

    var isEditing by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pin Passcode Security", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Lock household financials upon app boot with a 4-digit numeric PIN code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    if (!isEditing) {
                        val hasPin = settings != null && settings.pinCode.isNotEmpty()
                        Text(
                            text = if (hasPin) "Status: SECURED ACTIVE PIN LOCK ON" else "Status: PIN PASSCODE DISABLED",
                            fontWeight = FontWeight.Bold,
                            color = if (hasPin) Color(0xFF2ec4b6) else Color(0xFFe71d36),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { isEditing = true }, modifier = Modifier.testTag("toggle_pin_edit")) {
                            Text(if (hasPin) "Configure/Change PIN" else "Configure PIN Lock")
                        }
                    } else {
                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { if (it.length <= 4) pinCode = it },
                            label = { Text("4-digit Numeric PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("pin_code_field")
                        )

                        OutlinedTextField(
                            value = securityQ,
                            onValueChange = { securityQ = it },
                            label = { Text("Security Recovery Question") },
                            placeholder = { Text("e.g. What is your first pet's name?") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = securityA,
                            onValueChange = { securityA = it },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateLockSettings(pinCode, securityQ, securityA)
                                    isEditing = false
                                    viewModel.fireLocalNotification("Security Restructured", "PIN configurations updated securely.")
                                },
                                modifier = Modifier.weight(1f).testTag("save_security_btn")
                            ) {
                                Text("Save Settings")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.updateLockSettings("", "", "")
                                    pinCode = ""
                                    securityQ = ""
                                    securityA = ""
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disable PIN")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    viewModel: HouseholdViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("Son") }
    var notes by remember { mutableStateOf("") }

    val colors = listOf("#FF6200EE", "#FF3700B3", "#FF03DAC5", "#FF2196F3", "#FFAEEA00", "#FFFFD600", "#FFD50000")
    var badgeColorHex by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Family Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_member_name")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Relationship: ", fontWeight = FontWeight.Bold)
                    var relDrop by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { relDrop = true }) {
                            Text(relation)
                        }
                        DropdownMenu(expanded = relDrop, onDismissRequest = { relDrop = false }) {
                            listOf("Spouse", "Mother", "Father", "Grandparent", "Son", "Daughter", "Relative").forEach { r ->
                                DropdownMenuItem(text = { Text(r) }, onClick = { relation = r; relDrop = false })
                            }
                        }
                    }
                }

                Text("Design Badge Color:", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { badgeColorHex = hex }
                                .border(
                                    width = if (badgeColorHex == hex) 2.dp else 0.dp,
                                    color = if (badgeColorHex == hex) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Bio notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val m = FamilyMember(
                            name = name.trim(),
                            relation = relation,
                            notes = notes.trim(),
                            badgeColorHex = badgeColorHex
                        )
                        viewModel.addFamilyMember(m)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("submit_member")
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddBudgetDialog(
    viewModel: HouseholdViewModel,
    onDismiss: () -> Unit
) {
    val symbol = viewModel.getCurrencySymbol()
    var category by remember { mutableStateOf("Overall") }
    var limitStr by remember { mutableStateOf("") }
    var budgetMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var budgetYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup Spending limit", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Limit Category: ", fontWeight = FontWeight.Bold)
                    var catDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { catDropdown = true }) {
                            Text(category)
                        }
                        DropdownMenu(expanded = catDropdown, onDismissRequest = { catDropdown = false }) {
                            listOf("Overall", "Grocery", "Utility", "Health", "Clothes", "Personal").forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; catDropdown = false })
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Budget Limit ($symbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_budget_limit")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    var mDrop by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { mDrop = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Month: $budgetMonth")
                        }
                        DropdownMenu(expanded = mDrop, onDismissRequest = { mDrop = false }) {
                            (1..12).forEach { m ->
                                DropdownMenuItem(text = { Text("$m (${getMonthName(m)})") }, onClick = { budgetMonth = m; mDrop = false })
                            }
                        }
                    }

                    var yDrop by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { yDrop = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Year: $budgetYear")
                        }
                        DropdownMenu(expanded = yDrop, onDismissRequest = { yDrop = false }) {
                            (2025..2030).forEach { y ->
                                DropdownMenuItem(text = { Text("$y") }, onClick = { budgetYear = y; yDrop = false })
                            }
                        }
                    }
                }

                if (showError) {
                    Text("Define a valid spending amount.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitStr.toDoubleOrNull()
                    if (limit != null && limit > 0) {
                        val b = Budget(
                            category = category,
                            month = budgetMonth,
                            year = budgetYear,
                            budgetLimit = limit
                        )
                        viewModel.addBudget(b)
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("submit_budget")
            ) {
                Text("Activate limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
