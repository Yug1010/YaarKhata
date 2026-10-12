package com.yaarkhata.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yaarkhata.app.ui.navigation.Screen

// The four persistent tabs from the screen flow: Dashboard, Accounts, Debts, Reports.
// Person detail, Add entry, Add debt, and Record payment are pushed screens without this bar.
@Composable
fun BottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    val items = listOf(
        Triple(Screen.Dashboard.route, "Home", Icons.Filled.Home),
        Triple(Screen.Accounts.route, "Accounts", Icons.Filled.AccountBalanceWallet),
        Triple(Screen.Debts.route, "Debts", Icons.Filled.Groups),
        Triple(Screen.Reports.route, "Reports", Icons.Filled.BarChart)
    )

    NavigationBar {
        items.forEach { (route, label, icon) ->
            val selected = currentRoute?.hierarchy?.any { it.route == route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
