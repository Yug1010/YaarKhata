package com.yaarkhata.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.data.DebtDirection
import com.yaarkhata.app.data.Transaction
import com.yaarkhata.app.repository.FinanceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(repository: FinanceRepository, onAddEntry: () -> Unit) {
    val netWorth by repository.getNetWorth().collectAsStateWithLifecycle(initialValue = 0.0)
    val accounts by repository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val debts by repository.getDebts().collectAsStateWithLifecycle(initialValue = emptyList())
    val expenses by repository.getExpenses().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())
    var editingExpense by remember { mutableStateOf<Transaction?>(null) }
    val scope = rememberCoroutineScope()

    val bankTotal = accounts.filter { it.type.name == "BANK" }.sumOf { it.balance }
    val cashTotal = accounts.filter { it.type.name == "CASH" }.sumOf { it.balance }
    val owedToMe = debts.filter { it.direction == DebtDirection.OWED_TO_ME }.sumOf { it.remainingAmount }
    val iOwe = debts.filter { it.direction == DebtDirection.I_OWE }.sumOf { it.remainingAmount }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = onAddEntry) { Icon(Icons.Filled.Add, contentDescription = "Add entry") }
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Net worth", style = MaterialTheme.typography.labelMedium)
            Text("Rs ${"%.0f".format(netWorth)}", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Bank accounts", bankTotal, Modifier.weight(1f))
                MetricCard("Cash", cashTotal, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Pending debts", style = MaterialTheme.typography.titleSmall)
                    Text("Owed to you: Rs ${"%.0f".format(owedToMe)}  -  You owe: Rs ${"%.0f".format(iOwe)}")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Recent expenses", style = MaterialTheme.typography.titleSmall)
            expenses.take(5).forEach { tx ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tx.note ?: "Expense")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-Rs ${"%.0f".format(tx.amount)}")
                            IconButton(onClick = { editingExpense = tx }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit expense")
                            }
                        }
                    }
                }
            }
        }
    }

    editingExpense?.let { tx ->
        EditExpenseDialog(
            transaction = tx,
            accounts = accounts,
            categories = categories,
            onDismiss = { editingExpense = null },
            onSave = { newAmount, newAccountId, newCategoryId, newNote ->
                scope.launch {
                    repository.editTransaction(tx.id, newAmount, newAccountId, newCategoryId, newNote)
                    editingExpense = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExpenseDialog(
    transaction: Transaction,
    accounts: List<com.yaarkhata.app.data.Account>,
    categories: List<com.yaarkhata.app.data.Category>,
    onDismiss: () -> Unit,
    onSave: (Double, Long, Long?, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("%.0f".format(transaction.amount)) }
    var accountId by remember { mutableStateOf<Long?>(transaction.accountId) }
    var categoryText by remember {
        mutableStateOf(categories.firstOrNull { it.id == transaction.categoryId }?.name ?: "")
    }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit expense") },
        text = {
            Column {
                OutlinedTextField(value = amountText, onValueChange = { amountText = it; error = null }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(12.dp))
                Text("Account", style = MaterialTheme.typography.labelMedium)
                Row {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = accountId == acc.id,
                            onClick = { accountId = acc.id; error = null },
                            label = { Text(acc.name) },
                            modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it; categoryMenuExpanded = true },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    val matches = categories.filter { it.name.contains(categoryText, ignoreCase = true) }
                    if (matches.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                            matches.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.name) }, onClick = {
                                    categoryText = cat.name
                                    categoryMenuExpanded = false
                                })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amountText.toDoubleOrNull()
                val acc = accountId
                when {
                    amt == null || amt <= 0.0 -> error = "Enter a valid amount"
                    acc == null -> error = "Choose an account"
                    else -> {
                        val categoryId = categories.firstOrNull { it.name.equals(categoryText.trim(), ignoreCase = true) }?.id
                        onSave(amt, acc, categoryId, note.ifBlank { null })
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MetricCard(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("Rs ${"%.0f".format(value)}", style = MaterialTheme.typography.titleMedium)
        }
    }
}
