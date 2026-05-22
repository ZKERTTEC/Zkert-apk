package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AccountingDatabase
import com.example.data.repository.AccountingRepository
import com.example.ui.AccountingViewModel
import com.example.ui.AccountingViewModelFactory
import com.example.ui.screens.ChartOfAccountsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JournalEntryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TreasuryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate900

enum class AppScreen(val route: String, val title: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "الرئيسية", Icons.Default.Home),
    CHART("chart", "الحسابات", Icons.Default.List),
    JOURNAL("journal", "قيد جديد", Icons.Default.Add),
    TREASURY("treasury", "سند سريع", Icons.Default.Check),
    REPORTS("reports", "التقارير", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                // Database injection layers (Clean VM Factory setup)
                val database = remember { AccountingDatabase.getDatabase(applicationContext) }
                val repository = remember { AccountingRepository(database.dao()) }
                val factory = remember { AccountingViewModelFactory(repository) }
                val viewModel: AccountingViewModel = viewModel(factory = factory)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        SleekTopAppBar()
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .testTag("accounting_app_bottom_nav")
                                .statusBarsPadding()
                                .height(72.dp),
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            // Render right-to-left order for ergonomic Arabic UX
                            AppScreen.values().reversed().forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF2563EB),
                                        selectedTextColor = Color(0xFF2563EB),
                                        indicatorColor = Color(0xFFEFF6FF), // very soft blue pill
                                        unselectedIconColor = Color(0xFF94A3B8),
                                        unselectedTextColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF8FAFC)) // Crisp #F8FAFC Tailwind/Sleek background
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToQuickVoucher = { currentScreen = AppScreen.TREASURY },
                                onNavigateToJournal = { currentScreen = AppScreen.JOURNAL }
                            )
                            AppScreen.CHART -> ChartOfAccountsScreen(viewModel = viewModel)
                            AppScreen.JOURNAL -> JournalEntryScreen(viewModel = viewModel)
                            AppScreen.TREASURY -> TreasuryScreen(viewModel = viewModel)
                            AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                        }

                        // Floating Offline status badge in the bottom-left corner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            OfflineFloatingBadge()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleekTopAppBar() {
    Surface(
        color = Slate900,
        contentColor = Color.White,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Edge-to-edge padding
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left header buttons (Search & Profile circular mock buttons)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔍", fontSize = 16.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", fontSize = 16.sp)
                    }
                }

                // Right header logo & title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "زكرت للمحاسبة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp,
                            fontSize = 18.sp
                        )
                    )

                    // Blue rounded block with logo letter "Z"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ز",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineFloatingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = Color(0xEB0F172A), // Dark slate trans
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80).copy(alpha = alpha))
            )
            Text(
                text = "وضع الأوفلاين نشط",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            )
        }
    }
}
