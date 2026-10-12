package com.yaarkhata.app.ui.monthreport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.repository.FinanceRepository
import java.text.SimpleDateFormat
import java.util.Locale

private val barColors = listOf(
    Color(0xFF1D9E75), Color(0xFF3B6D11), Color(0xFFA32D2D),
    Color(0xFF2D5AA3), Color(0xFF9E6B1D), Color(0xFF6B3B9E)
)

// monthKey is "yyyy-MM" - exact and locale-independent, unlike the display label.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthReportScreen(monthKey: String, monthLabel: String, repository: FinanceRepository, onBack: () -> Unit) {
    val allExpenses by repository.getExpenses().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())
    val monthKeyFmt = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    val categoryNames = categories.associate { it.id to it.name }
    val monthExpenses = allExpenses
        .filter { monthKeyFmt.format(it.date) == monthKey }
        .sortedByDescending { it.date }

    val total = monthExpenses.sumOf { it.amount }
    val totalsByCategory = monthExpenses
        .groupBy { categoryNames[it.categoryId] ?: "Uncategorized" }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
    val maxCategoryAmount = totalsByCategory.maxOfOrNull { it.second } ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(monthLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Total spending", style = MaterialTheme.typography.labelMedium)
                    Text("Rs ${"%.0f".format(total)}", style = MaterialTheme.typography.headlineSmall)
                }
            }

            if (totalsByCategory.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("By category", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                totalsByCategory.forEachIndexed { index, (name, amount) ->
                    val fraction = if (maxCategoryAmount > 0) (amount / maxCategoryAmount).toFloat() else 0f
                    val color = barColors[index % barColors.size]
                    Column(Modifier.padding(bottom = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                            Text("Rs ${"%.0f".format(amount)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(14.dp).background(Color(0xFFE5E3DC), RoundedCornerShape(7.dp))) {
                            Box(
                                Modifier
                                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                                    .height(14.dp)
                                    .background(color, RoundedCornerShape(7.dp))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("All transactions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (monthExpenses.isEmpty()) {
                Text("No expenses logged this month.", style = MaterialTheme.typography.bodyMedium)
            } else {
                monthExpenses.forEach { tx ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tx.note ?: "Expense")
                                Text("Rs ${"%.0f".format(tx.amount)}")
                            }
                            Text(
                                "${categoryNames[tx.categoryId] ?: "Uncategorized"} - ${dateFmt.format(tx.date)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
