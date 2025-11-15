package com.example.budgettrain

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import com.google.firebase.FirebaseApp
import com.example.budgettrain.data.repository.FirebaseAuthRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase (usually done automatically, but ensure it's initialized)
        FirebaseApp.initializeApp(this)
        
        // Check if user is logged in, if not redirect to login
        val authRepository = FirebaseAuthRepository()
        if (!authRepository.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }
        
        enableEdgeToEdge()
        setContent {
            BudgetTrainTheme {
                val navController = rememberNavController()
                val items = listOf(
                    BottomItem.Dashboard,
                    BottomItem.Expenses,
                    BottomItem.Reports,
                    BottomItem.Goals,
                    BottomItem.Rewards,
                    BottomItem.Logout
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
                                    label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
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
                        composable(BottomItem.Logout.route) {
                            val context = LocalContext.current
                            LogoutScreen(
                                onLogout = {
                                    val intent = Intent(context, LogoutActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    context.startActivity(intent)
                                    if (context is Activity) {
                                        context.finish()
                                    }
                                },
                                onStayLoggedIn = {
                                    navController.navigate(BottomItem.Dashboard.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

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

@Composable
fun LogoutScreen(
    onLogout: () -> Unit,
    onStayLoggedIn: () -> Unit
) {
    val authRepository = FirebaseAuthRepository()
    val currentUser = authRepository.currentUser
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Logout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = if (currentUser != null) {
                "You are currently logged in as:\n${currentUser.email ?: "User"}"
            } else {
                "You are currently logged in"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF757575),
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336)
            )
        ) {
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onStayLoggedIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = "Stay Logged In",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private sealed class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : BottomItem("dashboard", "Dashboard", Icons.Filled.Home)
    data object Expenses : BottomItem("expenses", "Expenses", Icons.Filled.ReceiptLong)
    data object Reports : BottomItem("reports", "Reports", Icons.Filled.Assessment)
    data object Goals : BottomItem("goals", "Goals", Icons.Filled.Flag)
    data object Rewards : BottomItem("rewards", "Rewards", Icons.Filled.Star)
    data object Logout : BottomItem("logout", "Logout", Icons.Filled.Flag)

}

 