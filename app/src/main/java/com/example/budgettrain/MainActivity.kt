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
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
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
import com.example.budgettrain.feature.expense.AddExpenseScreen
import com.example.budgettrain.feature.expense.ExpenseListScreen
import com.example.budgettrain.feature.rewards.RewardsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTrainTheme {
                val navController = rememberNavController()
                val items = listOf(
                    BottomItem.Dashboard,
                    BottomItem.Expenses,
                    BottomItem.Reports,
                    BottomItem.Goals,
                    BottomItem.Rewards
                )
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        if (currentRoute == BottomItem.Expenses.route) {
                            FloatingActionButton(onClick = { navController.navigate("add_expense") }) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = "Add Expense")
                            }
                        }
                    },
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
                                        navController.navigate(item.route) {
                                            // Pop up to the start destination of the graph to
                                            // avoid building up a large stack of destinations
                                            // on the back stack as users select items
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = false
                                            }
                                            // Avoid multiple copies of the same destination when
                                            // reselecting the same item
                                            launchSingleTop = true
                                            // Restore state when reselecting a previously selected item
                                            restoreState = true
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
                                onNavigateToAddExpense = { navController.navigate("add_expense") },
                                onNavigateToReports = { navController.navigate(BottomItem.Reports.route) },
                                onNavigateToGoals = { navController.navigate(BottomItem.Goals.route) }
                            ) 
                        }
                        composable(BottomItem.Expenses.route) { ExpenseListScreen() }
                        composable(BottomItem.Reports.route) { ReportsScreen() }
                        composable(BottomItem.Goals.route) { BudgetGoalsScreen() }
                        composable(BottomItem.Rewards.route) { RewardsScreen() }

                        composable("add_expense") {
                            AddExpenseScreen(
                                onSaved = { navController.popBackStack(); navController.navigate(BottomItem.Expenses.route) },
                                onViewExpenses = { navController.navigate(BottomItem.Expenses.route) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : BottomItem("dashboard", "Dashboard", Icons.Filled.Home)
    data object Expenses : BottomItem("expenses", "Expenses", Icons.Filled.ReceiptLong)
    data object Reports : BottomItem("reports", "Reports", Icons.Filled.Assessment)
    data object Goals : BottomItem("goals", "Goals", Icons.Filled.Flag)
    data object Rewards : BottomItem("rewards", "Rewards", Icons.Filled.Star)

}

 