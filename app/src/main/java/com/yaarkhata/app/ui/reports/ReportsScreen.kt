package com.yaarkhata.app.ui.reports

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.repository.FinanceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val barColors = listOf(
    Color(0xFF1D9E75), Color(0xFF3B6D11), Color(0xFFA32D2D),
    Color(0xFF2D5AA3), Color(0xFF9E6B1D), Color(0xFF6B3B9E)
)

private enum class ReportScope { ALL_TIME, THIS_MONTH }

// Only pulls from the Transaction table (type = EXPENSE), never from Debt,
// so lending or borrowing money never shows up as "spending" here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    repository: FinanceRepository,
    onViewAdjustments: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenMonth: (String, String) -> Unit
) {
    val allExpenses by repository.getExpenses().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())
    var scope by remember { mutableStateOf(ReportScope.ALL_TIME) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<String?>(null) }
    val scopeCoroutine = rememberCoroutineScope()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scopeCoroutine.launch {
            try {
                val json = repository.exportDataAsJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scopeCoroutine.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json != null) showImportConfirm = json
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't read that file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val categoryNames = categories.associate { it.id to it.name }
    val monthKeyFmt = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val monthLabelFmt = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val currentMonthKey = remember { monthKeyFmt.format(Calendar.getInstance().time) }

    val expenses = if (scope == ReportScope.THIS_MONTH) {
        allExpenses.filter { monthKeyFmt.format(it.date) == currentMonthKey }
    } else {
        allExpenses
    }

    val totalsByCategory = expenses
        .groupBy { categoryNames[it.categoryId] ?: "Uncategorized" }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val total = expenses.sumOf { it.amount }
    val maxCategoryAmount = totalsByCategory.maxOfOrNull { it.second } ?: 0.0

    // Always all-time, regardless of the toggle above - this is the trend
    // view, so restricting it to "this month" would leave nothing to compare.
    val totalsByMonth = allExpenses
        .groupBy { monthKeyFmt.format(it.date) }
        .toList()
        .sortedBy { it.first }
        .takeLast(12)
        .map { (key, txs) -> Triple(key, monthLabelFmt.format(monthKeyFmt.parse(key)!!), txs.sumOf { it.amount }) }
    val maxMonthAmount = totalsByMonth.maxOfOrNull { it.third } ?: 0.0

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Reports", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = scope == ReportScope.ALL_TIME, onClick = { scope = ReportScope.ALL_TIME }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("All time") }
            SegmentedButton(selected = scope == ReportScope.THIS_MONTH, onClick = { scope = ReportScope.THIS_MONTH }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("This month") }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(if (scope == ReportScope.ALL_TIME) "Total spending, all time" else "Total spending, this month", style = MaterialTheme.typography.labelMedium)
                Text("Rs ${"%.0f".format(total)}", style = MaterialTheme.typography.headlineSmall)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("By category", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text("Tap a category to see its transactions.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        if (totalsByCategory.isEmpty()) {
            Text("No expenses logged yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            totalsByCategory.forEachIndexed { index, (name, amount) ->
                val fraction = if (maxCategoryAmount > 0) (amount / maxCategoryAmount).toFloat() else 0f
                val color = barColors[index % barColors.size]
                Column(
                    Modifier
                        .padding(bottom = 12.dp)
                        .clickable { onOpenCategory(name) }
                ) {
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
        Text("By month", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text("Tap a month to see its full report.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        if (totalsByMonth.isEmpty()) {
            Text("No expenses logged yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            totalsByMonth.forEach { (key, label, amount) ->
                val fraction = if (maxMonthAmount > 0) (amount / maxMonthAmount).toFloat() else 0f
                Column(
                    Modifier
                        .padding(bottom = 12.dp)
                        .clickable { onOpenMonth(key, label) }
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text("Rs ${"%.0f".format(amount)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(14.dp).background(Color(0xFFE5E3DC), RoundedCornerShape(7.dp))) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                                .height(14.dp)
                                .background(Color(0xFF1D9E75), RoundedCornerShape(7.dp))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onViewAdjustments, modifier = Modifier.fillMaxWidth()) {
            Text("View adjustments")
        }

        Spacer(Modifier.height(24.dp))
        Text("Backup", style = MaterialTheme.typography.titleSmall)
        Text(
            "This app keeps everything only on this device. Export a backup before switching phones, or to restore one later.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().time)
                    exportLauncher.launch("yaarkhata_backup_$stamp.json")
                },
                modifier = Modifier.weight(1f)
            ) { Text("Export") }
            OutlinedButton(
                onClick = { importPickerLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f)
            ) { Text("Import") }
        }

        Spacer(Modifier.height(24.dp))
        // Deliberately low-emphasis and tucked at the very bottom - not
        // something that should be easy to trigger by accident.
        TextButton(onClick = { showResetConfirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset all app data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all app data?") },
            text = { Text("This permanently deletes every account, transaction, debt, and payment. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scopeCoroutine.launch {
                        repository.resetAllData()
                        showResetConfirm = false
                    }
                }) { Text("Delete everything") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } }
        )
    }

    showImportConfirm?.let { json ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text("Restore this backup?") },
            text = { Text("This replaces everything currently in the app with the contents of the selected file. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scopeCoroutine.launch {
                        try {
                            repository.importDataFromJson(json)
                            Toast.makeText(context, "Backup restored", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        showImportConfirm = null
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = null }) { Text("Cancel") } }
        )
    }
}
