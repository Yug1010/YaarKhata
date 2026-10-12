package com.yaarkhata.app.ui.adddebt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.data.DebtDirection
import com.yaarkhata.app.repository.FinanceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(repository: FinanceRepository, onDone: () -> Unit) {
    val people by repository.getPeople().collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by repository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())

    var direction by remember { mutableStateOf(DebtDirection.OWED_TO_ME) }
    var personName by remember { mutableStateOf("") }
    var personMenuExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var categoryText by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Add debt", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = direction == DebtDirection.OWED_TO_ME, onClick = { direction = DebtDirection.OWED_TO_ME; error = null }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Owes me") }
            SegmentedButton(selected = direction == DebtDirection.I_OWE, onClick = { direction = DebtDirection.I_OWE; error = null }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("I owe") }
        }

        Spacer(Modifier.height(12.dp))
        // Dropdown of existing people, but typing a new name and saving
        // still creates them - same search-or-add pattern used elsewhere.
        ExposedDropdownMenuBox(expanded = personMenuExpanded, onExpandedChange = { personMenuExpanded = it }) {
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it; error = null; personMenuExpanded = true },
                label = { Text("Person") },
                placeholder = { Text("Search or add a name") },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            val matches = people.filter { it.name.contains(personName, ignoreCase = true) }
            if (matches.isNotEmpty()) {
                ExposedDropdownMenu(expanded = personMenuExpanded, onDismissRequest = { personMenuExpanded = false }) {
                    matches.forEach { person ->
                        DropdownMenuItem(text = { Text(person.name) }, onClick = {
                            personName = person.name
                            personMenuExpanded = false
                        })
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it; error = null }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())

        if (direction == DebtDirection.OWED_TO_ME) {
            // You're fronting the money right now - it leaves a real account immediately.
            Spacer(Modifier.height(12.dp))
            Text("From account", style = MaterialTheme.typography.labelMedium)
            accounts.forEach { acc ->
                FilterChip(
                    selected = accountId == acc.id,
                    onClick = { accountId = acc.id; error = null },
                    label = { Text(acc.name) },
                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                )
            }
            Text("This leaves your account now.", style = MaterialTheme.typography.bodySmall)
        } else {
            // Nothing has left your pocket yet, but this IS your expense -
            // someone else just fronted it. Categorize it now, same as any
            // other expense, so the eventual repayment lands in the right
            // place in Reports instead of a generic "debt" bucket.
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it; error = null; categoryMenuExpanded = true },
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
            Text(
                "No account needed yet - someone else paid, so nothing has left your pocket until you settle up.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val amt = amount.toDoubleOrNull()
            when {
                personName.isBlank() -> error = "Enter a person's name"
                amt == null || amt <= 0.0 -> error = "Enter a valid amount"
                direction == DebtDirection.OWED_TO_ME && accountId == null -> error = "Choose which account this came from"
                direction == DebtDirection.I_OWE && categoryText.isBlank() -> error = "Choose a category"
                else -> {
                    scope.launch {
                        val personId = repository.findOrCreatePerson(personName.trim())
                        val categoryId = if (direction == DebtDirection.I_OWE) {
                            repository.findOrCreateCategory(categoryText.trim())
                        } else null
                        repository.addDebt(personId, accountId, categoryId, amt, direction, description.ifBlank { "Debt" })
                        onDone()
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Save debt")
        }
    }
}
