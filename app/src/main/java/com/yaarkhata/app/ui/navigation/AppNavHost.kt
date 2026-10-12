package com.yaarkhata.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yaarkhata.app.repository.FinanceRepository
import com.yaarkhata.app.ui.accounts.AccountsScreen
import com.yaarkhata.app.ui.adddebt.AddDebtScreen
import com.yaarkhata.app.ui.addentry.AddEntryScreen
import com.yaarkhata.app.ui.adjustments.AdjustmentsScreen
import com.yaarkhata.app.ui.categorytransactions.CategoryTransactionsScreen
import com.yaarkhata.app.ui.components.BottomNavBar
import com.yaarkhata.app.ui.dashboard.DashboardScreen
import com.yaarkhata.app.ui.debts.DebtsScreen
import com.yaarkhata.app.ui.monthreport.MonthReportScreen
import com.yaarkhata.app.ui.persondetail.PersonDetailScreen
import com.yaarkhata.app.ui.recordpayment.RecordPaymentScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object Debts : Screen("debts")
    object Reports : Screen("reports")
    object AddEntry : Screen("add_entry")
    object AddDebt : Screen("add_debt")
    object Adjustments : Screen("adjustments")
    object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: Long) = "person_detail/$personId"
    }
    object RecordPayment : Screen("record_payment/{debtId}") {
        fun createRoute(debtId: Long) = "record_payment/$debtId"
    }
    object CategoryTransactions : Screen("category_transactions/{categoryName}") {
        fun createRoute(categoryName: String) = "category_transactions/${android.net.Uri.encode(categoryName)}"
    }
    object MonthReport : Screen("month_report/{monthKey}/{monthLabel}") {
        fun createRoute(monthKey: String, monthLabel: String) =
            "month_report/${android.net.Uri.encode(monthKey)}/${android.net.Uri.encode(monthLabel)}"
    }
}

// Mirrors the screen-flow diagram: Dashboard fans out to the four tabs;
// Debts -> Person detail -> Record payment / Share via WhatsApp.
@Composable
fun AppNavHost(repository: FinanceRepository) {
    val navController = rememberNavController()

    Scaffold(bottomBar = { BottomNavBar(navController) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = androidx.compose.ui.Modifier.padding(padding).consumeWindowInsets(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(repository = repository, onAddEntry = { navController.navigate(Screen.AddEntry.route) })
            }
            composable(Screen.Accounts.route) {
                AccountsScreen(repository = repository)
            }
            composable(Screen.Debts.route) {
                DebtsScreen(
                    repository = repository,
                    onAddDebt = { navController.navigate(Screen.AddDebt.route) },
                    onOpenPerson = { personId -> navController.navigate(Screen.PersonDetail.createRoute(personId)) }
                )
            }
            composable(Screen.Reports.route) {
                com.yaarkhata.app.ui.reports.ReportsScreen(
                    repository = repository,
                    onViewAdjustments = { navController.navigate(Screen.Adjustments.route) },
                    onOpenCategory = { categoryName -> navController.navigate(Screen.CategoryTransactions.createRoute(categoryName)) },
                    onOpenMonth = { monthKey, monthLabel -> navController.navigate(Screen.MonthReport.createRoute(monthKey, monthLabel)) }
                )
            }
            composable(Screen.Adjustments.route) {
                AdjustmentsScreen(repository = repository, onBack = { navController.popBackStack() })
            }
            composable(
                Screen.CategoryTransactions.route,
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = android.net.Uri.decode(backStackEntry.arguments?.getString("categoryName") ?: "")
                CategoryTransactionsScreen(
                    categoryName = categoryName,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.MonthReport.route,
                arguments = listOf(
                    navArgument("monthKey") { type = NavType.StringType },
                    navArgument("monthLabel") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val monthKey = android.net.Uri.decode(backStackEntry.arguments?.getString("monthKey") ?: "")
                val monthLabel = android.net.Uri.decode(backStackEntry.arguments?.getString("monthLabel") ?: "")
                MonthReportScreen(
                    monthKey = monthKey,
                    monthLabel = monthLabel,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddEntry.route) {
                AddEntryScreen(repository = repository, onDone = { navController.popBackStack() })
            }
            composable(Screen.AddDebt.route) {
                AddDebtScreen(repository = repository, onDone = { navController.popBackStack() })
            }
            composable(
                Screen.PersonDetail.route,
                arguments = listOf(navArgument("personId") { type = NavType.LongType })
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId") ?: 0L
                PersonDetailScreen(
                    personId = personId,
                    repository = repository,
                    onRecordPayment = { debtId -> navController.navigate(Screen.RecordPayment.createRoute(debtId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.RecordPayment.route,
                arguments = listOf(navArgument("debtId") { type = NavType.LongType })
            ) { backStackEntry ->
                val debtId = backStackEntry.arguments?.getLong("debtId") ?: 0L
                RecordPaymentScreen(
                    debtId = debtId,
                    repository = repository,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
