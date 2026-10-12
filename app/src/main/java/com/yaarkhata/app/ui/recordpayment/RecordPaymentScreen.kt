package com.yaarkhata.app.ui.recordpayment

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
import com.yaarkhata.app.data.Debt
import com.yaarkhata.app.repository.FinanceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(debtId: Long, repository: FinanceRepository, onDone: () -> Unit) {
    val accounts by repository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    var debt by remember { mutableStateOf<Debt?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(debtId) {
        val d = repository.getDebtById(debtId)
        debt = d
        // Pre-fill with the full remaining amount - a partial payment is just
        // editing this number down, per the app's design.
        amount = d?.remainingAmount?.let { "%.0f".format(it) } ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record payment") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            debt?.let { d ->
                Text("${d.description} - Rs ${"%.0f".format(d.remainingAmount)} remaining", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(value = amount, onValueChange = { amount = it; error = null }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))
            // Chosen here, independent of anything picked when the debt was
            // created - this is the account the real money actually moves in or out of.
            Text("Account", style = MaterialTheme.typography.labelMedium)
            accounts.forEach { acc ->
                FilterChip(
                    selected = accountId == acc.id,
                    onClick = { accountId = acc.id; error = null },
                    label = { Text(acc.name) },
                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val amt = amount.toDoubleOrNull()
                val acc = accountId
                val remaining = debt?.remainingAmount ?: 0.0
                when {
                    amt == null || amt <= 0.0 -> error = "Enter a valid amount"
                    amt > remaining -> error = "Amount can't exceed what's remaining"
                    acc == null -> error = "Choose which account this settles in"
                    else -> {
                        scope.launch {
                            repository.recordPayment(debtId, acc, amt, note.ifBlank { null })
                            onDone()
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Save payment")
            }
        }
    }
}
