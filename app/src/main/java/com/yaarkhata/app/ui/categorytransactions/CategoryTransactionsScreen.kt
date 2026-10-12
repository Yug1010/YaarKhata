package com.yaarkhata.app.ui.categorytransactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.repository.FinanceRepository
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(categoryName: String, repository: FinanceRepository, onBack: () -> Unit) {
    val expenses by repository.getExpenses().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    val categoryNames = categories.associate { it.id to it.name }
    val matching = expenses
        .filter { (categoryNames[it.categoryId] ?: "Uncategorized") == categoryName }
        .sortedByDescending { it.date }
    val total = matching.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Total", style = MaterialTheme.typography.labelMedium)
                    Text("Rs ${"%.0f".format(total)}", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(Modifier.height(16.dp))
            if (matching.isEmpty()) {
                Text("No transactions in this category.", style = MaterialTheme.typography.bodyMedium)
            } else {
                matching.forEach { tx ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tx.note ?: "Expense")
                                Text("Rs ${"%.0f".format(tx.amount)}")
                            }
                            Text(fmt.format(tx.date), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
