package com.yaarkhata.app.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.data.Account
import com.yaarkhata.app.data.AccountType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(repository: com.yaarkhata.app.repository.FinanceRepository) {
    val accounts by repository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            // Bottom-right, the standard reachable spot for a one-handed thumb grip.
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add account")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Accounts", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            accounts.forEach { account ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(account.name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rs ${"%.0f".format(account.balance)}")
                            IconButton(onClick = { accountToDelete = account }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${account.name}")
                            }
                        }
                    }
                }
            }
            // Leave room so the last card isn't hidden behind the FAB.
            Spacer(Modifier.height(72.dp))
        }
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete ${account.name}?") },
            text = {
                Text(
                    "This removes the account and its Rs ${"%.0f".format(account.balance)} balance. " +
                        "Past transactions logged against it stay in your history but will no longer show an account name."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteAccount(account) }
                    accountToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { accountToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf<AccountType?>(null) }
        var opening by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add account") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Account name") })
                    Spacer(Modifier.height(8.dp))
                    Text("Type", style = MaterialTheme.typography.labelMedium)
                    Row {
                        FilterChip(selected = type == AccountType.BANK, onClick = { type = AccountType.BANK; error = null }, label = { Text("Bank") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = type == AccountType.CASH, onClick = { type = AccountType.CASH; error = null }, label = { Text("Cash") })
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Opening balance") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val balance = opening.toDoubleOrNull() ?: 0.0
                    val chosenType = type
                    when {
                        name.isBlank() -> error = "Enter an account name"
                        chosenType == null -> error = "Choose Bank or Cash"
                        else -> {
                            scope.launch { repository.addAccount(name, chosenType, balance) }
                            showDialog = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
