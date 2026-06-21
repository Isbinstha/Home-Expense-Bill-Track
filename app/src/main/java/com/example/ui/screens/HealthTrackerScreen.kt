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
import com.example.data.FamilyMember
import com.example.data.HealthCheckup
import com.example.data.MedicinePurchase
import com.example.ui.viewmodel.HouseholdViewModel
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthTrackerScreen(
    viewModel: HouseholdViewModel,
    modifier: Modifier = Modifier
) {
    val checkups by viewModel.healthCheckups.collectAsState()
    val medicines by viewModel.medicinePurchases.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()

    val showCheckupDialog by viewModel.showAddCheckupDialog.collectAsState()
    val showMedicineDialog by viewModel.showAddMedicineDialog.collectAsState()

    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    var activeSubTab by remember { mutableStateOf("Meds") } // "Meds", "Checkups"

    val totalCheckupCost = checkups.sumOf { it.totalCost }
    val totalMedicineCost = medicines.sumOf { it.totalAmount }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Medical Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Medicines Cost", style = MaterialTheme.typography.labelMedium)
                        Text("$symbol ${totalMedicineCost.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.height(30.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Doctor & Labs Cost", style = MaterialTheme.typography.labelMedium)
                        Text("$symbol ${totalCheckupCost.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-tabs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { activeSubTab = "Meds" },
                    modifier = Modifier.weight(1f).testTag("tab_meds"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == "Meds") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (activeSubTab == "Meds") Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Medicines Log")
                }

                Button(
                    onClick = { activeSubTab = "Checkups" },
                    modifier = Modifier.weight(1f).testTag("tab_checkups"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == "Checkups") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (activeSubTab == "Checkups") Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clinics & Visits")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeSubTab == "Meds") {
                // Medicines Tab
                if (medicines.isEmpty()) {
                    EmptyMedicalState("No medicine tracked yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(medicines, key = { it.id }) { med ->
                            MedicineRowItem(med = med, viewModel = viewModel)
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            } else {
                // Checkups Tab
                if (checkups.isEmpty()) {
                    EmptyMedicalState("No checkup or clinic visits visual logs yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(checkups, key = { it.id }) { cu ->
                            CheckupRowItem(cu = cu, viewModel = viewModel)
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Action FAB Trigger
        FloatingActionButton(
            onClick = {
                if (activeSubTab == "Meds") {
                    viewModel.showAddMedicineDialog.value = true
                } else {
                    viewModel.showAddCheckupDialog.value = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_health_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Medical log")
        }

        if (showMedicineDialog) {
            AddMedicineDialog(viewModel = viewModel, familyMembers = familyMembers, onDismiss = { viewModel.showAddMedicineDialog.value = false })
        }

        if (showCheckupDialog) {
            AddCheckupDialog(viewModel = viewModel, familyMembers = familyMembers, onDismiss = { viewModel.showAddCheckupDialog.value = false })
        }
    }
}

@Composable
fun EmptyMedicalState(msg: String) {
    Box(modifier = Modifier.fillMaxHeight(0.7f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Healing,
                contentDescription = "",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MedicineRowItem(med: MedicinePurchase, viewModel: HouseholdViewModel) {
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    Card(
        modifier = Modifier.fillMaxWidth().testTag("med_row_${med.id}"),
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
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(med.medicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("For: ${med.patientName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            if (med.isPrescribed) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE3F2FD))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Prescribed", color = Color(0xFF1565C0), style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$symbol ${med.totalAmount.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { viewModel.removeMedicinePurchase(med) },
                        modifier = Modifier.size(24.dp).testTag("delete_med_${med.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
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
                Text(
                    text = "Ailment: ${med.purpose}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${med.quantity} unit x $symbol${med.rate.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = viewModel.formatDate(med.purchaseDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (med.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Notes: ${med.notes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun CheckupRowItem(cu: HealthCheckup, viewModel: HouseholdViewModel) {
    val currency by viewModel.currentCurrency.collectAsState()
    val symbol = viewModel.getCurrencySymbol()

    Card(
        modifier = Modifier.fillMaxWidth().testTag("checkup_row_${cu.id}"),
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
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalInformation,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(cu.patientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${cu.checkupType} @ ${cu.hospitalOrClinic}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$symbol ${cu.totalCost.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { viewModel.removeHealthCheckup(cu) },
                        modifier = Modifier.size(24.dp).testTag("delete_checkup_${cu.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (cu.doctorName.isNotEmpty()) {
                    Text("Doctor: Dr. ${cu.doctorName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                if (cu.diagnosis.isNotEmpty()) {
                    Text("Diagnosis/Symptom: ${cu.diagnosis}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
                if (cu.notes.isNotEmpty()) {
                    Text("Notes: ${cu.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Visit: ${viewModel.formatDate(cu.date)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                cu.followUpDate?.let { fUp ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Follow up: ${viewModel.formatDate(fUp)}",
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMedicineDialog(
    viewModel: HouseholdViewModel,
    familyMembers: List<FamilyMember>,
    onDismiss: () -> Unit
) {
    val symbol = viewModel.getCurrencySymbol()
    var personName by remember { mutableStateOf("Self") }
    var medicineName by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Fever") }
    var isPrescribed by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Medicine Purchase", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = { Text("Medicine Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_medicine_name"),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Patient: ", fontWeight = FontWeight.Bold)
                    var pDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { pDropdown = true }) {
                            Text(personName)
                        }
                        DropdownMenu(expanded = pDropdown, onDismissRequest = { pDropdown = false }) {
                            (listOf("Self") + familyMembers.map { it.name }).forEach { member ->
                                DropdownMenuItem(text = { Text(member) }, onClick = { personName = member; pDropdown = false })
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Qty (packs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_med_qty")
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Rate per pack ($symbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f).testTag("input_med_rate")
                    )
                }

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose / Symptom Type") },
                    placeholder = { Text("Fever, Cough, Diabetes, Blood Pressure") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrescribed, onCheckedChange = { isPrescribed = it })
                    Text("Prescribed by medical practitioner?", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Dosage notes / instructions") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text("Please input numbers correctly for pack limits.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyStr.toIntOrNull()
                    val r = rateStr.toDoubleOrNull()
                    if (medicineName.isNotBlank() && q != null && r != null) {
                        val purchase = MedicinePurchase(
                            patientName = personName,
                            medicineName = medicineName.trim(),
                            purchaseDate = System.currentTimeMillis(),
                            quantity = q,
                            rate = r,
                            totalAmount = q * r,
                            purpose = purpose.trim(),
                            isPrescribed = isPrescribed,
                            notes = notes.trim()
                        )
                        viewModel.addMedicinePurchase(purchase)
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("submit_medicine")
            ) {
                Text("Reserve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCheckupDialog(
    viewModel: HouseholdViewModel,
    familyMembers: List<FamilyMember>,
    onDismiss: () -> Unit
) {
    val symbol = viewModel.getCurrencySymbol()
    var personName by remember { mutableStateOf("Self") }
    var hospitalClinic by remember { mutableStateOf("") }
    var checkupType by remember { mutableStateOf("Routine") }
    var doctorName by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var followUpMs by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Clinical / Doctor Visit", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Patient: ", fontWeight = FontWeight.Bold)
                    var pDropdown by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { pDropdown = true }) {
                            Text(personName)
                        }
                        DropdownMenu(expanded = pDropdown, onDismissRequest = { pDropdown = false }) {
                            (listOf("Self") + familyMembers.map { it.name }).forEach { member ->
                                DropdownMenuItem(text = { Text(member) }, onClick = { personName = member; pDropdown = false })
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = hospitalClinic,
                    onValueChange = { hospitalClinic = it },
                    label = { Text("Hospital or Clinic Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_clinic_name")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    var cDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { cDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Type: $checkupType")
                        }
                        DropdownMenu(expanded = cDropdown, onDismissRequest = { cDropdown = false }) {
                            listOf("Routine", "Dental", "Eye Exam", "Cardiac", "Pediatric", "OBGYN", "Lab Work").forEach { t ->
                                DropdownMenuItem(text = { Text(t) }, onClick = { checkupType = t; cDropdown = false })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("Doctor's Name") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("Diagnosis / Chief Complaint") },
                    placeholder = { Text("Fever symptoms, Routine checkup") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Consultation Cost ($symbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_checkup_cost")
                )

                // Follow up date choice
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Follow up clinic Visit: ", fontWeight = FontWeight.Bold)
                    var fMenu by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { fMenu = true }) {
                            Text(followUpMs?.let { viewModel.formatDate(it) } ?: "No follow up")
                        }
                        DropdownMenu(expanded = fMenu, onDismissRequest = { fMenu = false }) {
                            DropdownMenuItem(text = { Text("No follow up") }, onClick = { followUpMs = null; fMenu = false })
                            DropdownMenuItem(text = { Text("In 3 Days") }, onClick = { followUpMs = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000); fMenu = false })
                            DropdownMenuItem(text = { Text("In 7 Days") }, onClick = { followUpMs = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000); fMenu = false })
                            DropdownMenuItem(text = { Text("In 14 Days") }, onClick = { followUpMs = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000); fMenu = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Diagnosis details & notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text("Please input a valid cost amount.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = amountStr.toDoubleOrNull()
                    if (hospitalClinic.isNotBlank() && cost != null && cost >= 0) {
                        val visit = HealthCheckup(
                            patientName = personName,
                            date = System.currentTimeMillis(),
                            hospitalOrClinic = hospitalClinic.trim(),
                            checkupType = checkupType,
                            doctorName = doctorName.trim(),
                            diagnosis = diagnosis.trim(),
                            notes = notes.trim(),
                            totalCost = cost,
                            followUpDate = followUpMs
                        )
                        viewModel.addHealthCheckup(visit)
                        viewModel.fireLocalNotification("Health Visit Saved", "Stored clinic encounter for $personName at $hospitalClinic.")
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("submit_checkup")
            ) {
                Text("Log Encounter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
