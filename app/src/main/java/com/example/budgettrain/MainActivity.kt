package com.example.budgettrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budgettrain.ui.theme.BudgetTrainTheme
import com.example.budgettrain.feature.dashboard.DashboardScreen
import com.example.budgettrain.feature.reports.ReportsScreen
import com.example.budgettrain.feature.goals.BudgetGoalsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTrainTheme {
                val navController = rememberNavController()
                val items = listOf(
                    BottomItem.Dashboard,
                    BottomItem.Reports,
                    BottomItem.Goals
                )
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                    onClick = {
                                        if (currentDestination?.route != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomItem.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(BottomItem.Dashboard.route) {
                            DashboardScreen(
                                onAddExpense = { /* intentionally not implemented */ },
                                onViewReports = { navController.navigate(BottomItem.Reports.route) },
                                onManageGoals = { navController.navigate(BottomItem.Goals.route) },
                                onViewAllExpenses = { /* intentionally not implemented */ }
                            )
                        }
                        composable(BottomItem.Reports.route) { ReportsScreen() }
                        composable(BottomItem.Goals.route) { BudgetGoalsScreen() }
                    }
                }
            }
        }
    }
}

private sealed class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : BottomItem("dashboard", "Dashboard", Icons.Filled.Home)
    data object Reports : BottomItem("reports", "Reports", Icons.Filled.Assessment)
    data object Goals : BottomItem("goals", "Goals", Icons.Filled.Flag)
}

 