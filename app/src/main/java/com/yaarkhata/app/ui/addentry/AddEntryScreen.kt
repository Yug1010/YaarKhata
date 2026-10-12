package com.yaarkhata.app.ui.addentry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.data.TransactionType
import com.yaarkhata.app.repository.FinanceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(repository: FinanceRepository, onDone: () -> Unit) {
    val accounts by repository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())

    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var categoryText by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    // Only relevant when type == ADJUSTMENT - e.g. a mutual fund autopay is
    // a subtraction that should never show up as a category expense.
    var isAddition by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Add entry", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TransactionType.entries.forEachIndexed { index, t ->
                SegmentedButton(
                    selected = type == t,
                    onClick = { type = t },
                    shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size)
                ) { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
            }
        }

        if (type == TransactionType.ADJUSTMENT) {
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = isAddition, onClick = { isAddition = true }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Add money") }
                SegmentedButton(selected = !isAddition, onClick = { isAddition = false }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Subtract money") }
            }
            Text(
                "e.g. dad sends cash (Add), a mutual fund autopay (Subtract). Never counted as spending.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it; error = null }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        Text("Account", style = MaterialTheme.typography.labelMedium)
        accounts.forEach { acc ->
            FilterChip(
                selected = accountId == acc.id,
                onClick = { accountId = acc.id },
                label = { Text(acc.name) },
                modifier = Modifier.padding(end = 8.dp, top = 4.dp)
            )
        }

        // Categories mainly help with expense reports, so only show the
        // picker for expenses - income/adjustments don't need one.
        if (type == TransactionType.EXPENSE) {
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it; categoryMenuExpanded = true },
                    label = { Text("Category") },
                    placeholder = { Text("Search or add a category") },
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
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val amt = amount.toDoubleOrNull()
            val acc = accountId
            when {
                amt == null || amt <= 0.0 -> error = "Enter a valid amount"
                acc == null -> error = "Add an account first"
                else -> {
                    scope.launch {
                        val categoryId = if (type == TransactionType.EXPENSE && categoryText.isNotBlank()) {
                            repository.findOrCreateCategory(categoryText.trim())
                        } else null
                        repository.addTransaction(
                            accountId = acc,
                            amount = amt,
                            type = type,
                            categoryId = categoryId,
                            note = note.ifBlank { null },
                            isAddition = isAddition
                        )
                        onDone()
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Save entry")
        }
    }
}
