package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HouseholdViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppScreenMainShell()
            }
        }
    }
}

@Composable
fun AppScreenMainShell() {
    val viewModel: HouseholdViewModel = viewModel()
    val lockSettings by viewModel.lockSettings.collectAsState()
    val isLocked by viewModel.isAppLocked.collectAsState()

    // Decide if we should render PIN lock entry screen
    val hasPin = lockSettings != null && lockSettings?.pinCode?.isNotEmpty() == true

    if (hasPin && isLocked) {
        PinLockEntryScreen(viewModel = viewModel, pinCode = lockSettings?.pinCode ?: "", securityQ = lockSettings?.securityQuestion ?: "", securityA = lockSettings?.securityAnswer ?: "")
    } else {
        MainAppContentShell(viewModel = viewModel)
    }
}

@Composable
fun PinLockEntryScreen(
    viewModel: HouseholdViewModel,
    pinCode: String,
    securityQ: String,
    securityA: String
) {
    var pinEntered by remember { mutableStateOf("") }
    var securityAnswerEntered by remember { mutableStateOf("") }
    var isRecoveryActive by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF1F2035), Color(0xFF0F101A))))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isRecoveryActive) {
                Text(
                    text = "Household Vault Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Input secure passcode to proceed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Dots representing code length
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    (1..4).forEach { idx ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pinEntered.length >= idx)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.White.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                if (showError) {
                    Text(
                        text = "Incorrect passcode. Try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Numeric keyboard
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "Del")
                    )

                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { char ->
                                KeypadButton(char) {
                                    when (char) {
                                        "C" -> pinEntered = ""
                                        "Del" -> if (pinEntered.isNotEmpty()) pinEntered = pinEntered.dropLast(1)
                                        else -> {
                                            if (pinEntered.length < 4) {
                                                pinEntered += char
                                            }
                                            if (pinEntered.length == 4) {
                                                if (pinEntered == pinCode) {
                                                    viewModel.isAppLocked.value = false
                                                } else {
                                                    showError = true
                                                    pinEntered = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Recover text
                if (securityQ.isNotEmpty()) {
                    Text(
                        text = "Forgot passcode?",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clickable { isRecoveryActive = true }
                            .testTag("forgot_pin_trigger")
                    )
                }
            } else {
                Text(
                    text = "Security Recovery Answer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = securityQ,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                OutlinedTextField(
                    value = securityAnswerEntered,
                    onValueChange = { securityAnswerEntered = it },
                    placeholder = { Text("Answer Here") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("recovery_answer_input")
                )

                if (showError) {
                    Text(
                        text = "Recovery answer is incorrect. Try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        if (securityAnswerEntered.trim().equals(securityA.trim(), ignoreCase = true)) {
                            viewModel.isAppLocked.value = false // Unlock
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_submit_btn")
                ) {
                    Text("Validate & Unlock")
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { isRecoveryActive = false; showError = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun KeypadButton(char: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .testTag("keypad_$char"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppContentShell(viewModel: HouseholdViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeUser by viewModel.currentProfileName.collectAsState()

    val reminders by viewModel.pendingReminders.collectAsState()
    val hasUnpaidAlerts = reminders.any { it.isOverdue }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val initials = if (activeUser.isNotEmpty()) {
                            val parts = activeUser.split(" ")
                            if (parts.size >= 2) {
                                "${parts[0].take(1).uppercase()}${parts[1].take(1).uppercase()}"
                            } else {
                                activeUser.take(2).uppercase()
                            }
                        } else "JD"
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HOUSEHOLD ACCOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Good Morning, $activeUser",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Notification alerts Box matching the w-12 h-12 design
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.currentScreen.value = "profile" }
                            .testTag("reminders_alerts_bell"),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (hasUnpaidAlerts) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("!")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Active Notifications Pending",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            }
        },
        bottomBar = {
            // Under bottom bars, let's respect safe system navigation spacing in Material 3
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                NavigationBar(
                    modifier = Modifier.fillMaxWidth().testTag("app_navigation_bar"),
                    tonalElevation = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    listOf(
                        NavigationTabItem("Dashboard", Icons.Default.Home, Icons.Outlined.Home, "dashboard"),
                        NavigationTabItem("Expenses", Icons.Default.ShoppingCart, Icons.Outlined.ShoppingCart, "groceries"),
                        NavigationTabItem("Utility", Icons.Default.Bolt, Icons.Outlined.OfflineBolt, "bills"),
                        NavigationTabItem("Health", Icons.Default.Healing, Icons.Outlined.Healing, "health"),
                        NavigationTabItem("Calendar", Icons.Default.CalendarMonth, Icons.Outlined.CalendarMonth, "calendar"),
                        NavigationTabItem("Insights", Icons.Default.BarChart, Icons.Outlined.BarChart, "insights"),
                        NavigationTabItem("Settings", Icons.Default.Settings, Icons.Outlined.Settings, "profile")
                    ).forEach { tab ->
                        val isSelected = currentScreen == tab.routeId
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.currentScreen.value = tab.routeId },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.routeId}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Animated transition layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel, onNavigate = { viewModel.currentScreen.value = it })
                    "groceries" -> ExpenseTrackerScreen(viewModel = viewModel)
                    "bills" -> BillTrackerScreen(viewModel = viewModel)
                    "health" -> HealthTrackerScreen(viewModel = viewModel)
                    "calendar" -> CalendarScreen(viewModel = viewModel)
                    "insights" -> InsightsScreen(viewModel = viewModel)
                    "profile" -> SettingsScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel, onNavigate = { viewModel.currentScreen.value = it })
                }
            }
        }
    }
}

data class NavigationTabItem(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val routeId: String
)
