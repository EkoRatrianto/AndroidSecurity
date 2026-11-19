package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ConsentPermissionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailedAssessmentScreen
import com.example.ui.screens.GuidanceScreen
import com.example.ui.screens.HistoricalReportsScreen
import com.example.ui.screens.SettingsPrivacyScreen
import com.example.ui.screens.WelcomeDisclosureScreen
import com.example.ui.theme.JagaPerangkatTheme
import com.example.ui.viewmodel.SecurityUiState
import com.example.ui.viewmodel.SecurityViewModel

object JagaRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_ASSESSMENT = "assessment"
    const val ROUTE_GUIDANCE = "guidance"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val mainDestinations = listOf(
    NavDestinationItem(
        route = JagaRoutes.ROUTE_DASHBOARD,
        label = "Beranda",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    ),
    NavDestinationItem(
        route = JagaRoutes.ROUTE_ASSESSMENT,
        label = "Rincian",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment
    ),
    NavDestinationItem(
        route = JagaRoutes.ROUTE_GUIDANCE,
        label = "Panduan",
        selectedIcon = Icons.Filled.Checklist,
        unselectedIcon = Icons.Outlined.Checklist
    ),
    NavDestinationItem(
        route = JagaRoutes.ROUTE_HISTORY,
        label = "Riwayat",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    NavDestinationItem(
        route = JagaRoutes.ROUTE_SETTINGS,
        label = "Privasi",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JagaPerangkatTheme {
                JagaPerangkatApp()
            }
        }
    }
}

@Composable
fun JagaPerangkatApp(
    viewModel: SecurityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.preferences.hasAcceptedDisclosure) {
        if (uiState.isConsentStepVisible) {
            ConsentPermissionScreen(
                preferences = uiState.preferences,
                onToggleAppScan = viewModel::setAppScanEnabled,
                onToggleNetworkScan = viewModel::setNetworkScanEnabled,
                onBack = viewModel::backToWelcome,
                onAcceptAndStart = viewModel::acceptDisclosureAndStart
            )
        } else {
            WelcomeDisclosureScreen(
                onContinueToConsent = viewModel::showConsentScreen
            )
        }
    } else {
        MainAppShell(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppShell(
    uiState: SecurityUiState,
    viewModel: SecurityViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: JagaRoutes.ROUTE_DASHBOARD

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "JagaPerangkat",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = Color.White) {
                        mainDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = Color.White) {
                        mainDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = JagaRoutes.ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(JagaRoutes.ROUTE_DASHBOARD) {
                        DashboardScreen(
                            uiState = uiState,
                            onRunQuickScan = viewModel::runSecurityAudit,
                            onOpenDetailedAssessment = { navigateTo(JagaRoutes.ROUTE_ASSESSMENT) },
                            onOpenFindings = { navigateTo(JagaRoutes.ROUTE_ASSESSMENT) },
                            onOpenNetworkAnalysis = { navigateTo(JagaRoutes.ROUTE_ASSESSMENT) },
                            onOpenGuidedRecommendations = { navigateTo(JagaRoutes.ROUTE_GUIDANCE) },
                            onOpenHistory = { navigateTo(JagaRoutes.ROUTE_HISTORY) }
                        )
                    }
                    composable(JagaRoutes.ROUTE_ASSESSMENT) {
                        DetailedAssessmentScreen(
                            uiState = uiState,
                            onRunScan = viewModel::runSecurityAudit
                        )
                    }
                    composable(JagaRoutes.ROUTE_GUIDANCE) {
                        GuidanceScreen(
                            uiState = uiState,
                            onToggleGuidedCheck = viewModel::toggleGuidedCheck
                        )
                    }
                    composable(JagaRoutes.ROUTE_HISTORY) {
                        HistoricalReportsScreen(
                            history = uiState.history,
                            onDeleteRecord = viewModel::deleteHistoryRecord
                        )
                    }
                    composable(JagaRoutes.ROUTE_SETTINGS) {
                        SettingsPrivacyScreen(
                            uiState = uiState,
                            onToggleAppScan = viewModel::setAppScanEnabled,
                            onToggleNetworkScan = viewModel::setNetworkScanEnabled,
                            onClearAllData = viewModel::clearAllLocalData
                        )
                    }
                }
            }
        }
    }
}
