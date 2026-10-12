package com.yaarkhata.app.ui.adjustments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

// A dedicated space for adjustments (money added or subtracted outside of
// normal income/expense - e.g. dad sending cash, or a mutual fund autopay)
// since they're intentionally left out of Reports, they'd otherwise be
// invisible anywhere in the app once made.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentsScreen(repository: FinanceRepository, onBack: () -> Unit) {
    val adjustments by repository.getAdjustments().collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by repository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    val totalAdded = adjustments.filter { it.isAddition }.sumOf { it.amount }
    val totalSubtracted = adjustments.filter { !it.isAddition }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adjustments") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Added", style = MaterialTheme.typography.labelMedium)
                        Text("Rs ${"%.0f".format(totalAdded)}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF3B6D11))
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Subtracted", style = MaterialTheme.typography.labelMedium)
                        Text("Rs ${"%.0f".format(totalSubtracted)}", style = MaterialTheme.typography.titleMedium, color = Color(0xFFA32D2D))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (adjustments.isEmpty()) {
                Text("No adjustments logged yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                adjustments.forEach { tx ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(tx.note ?: "Adjustment")
                                val color = if (tx.isAddition) Color(0xFF3B6D11) else Color(0xFFA32D2D)
                                val sign = if (tx.isAddition) "+" else "-"
                                Text("$sign Rs ${"%.0f".format(tx.amount)}", color = color)
                            }
                            Text(
                                "${accountNames[tx.accountId] ?: "Unknown account"} - ${fmt.format(tx.date)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
