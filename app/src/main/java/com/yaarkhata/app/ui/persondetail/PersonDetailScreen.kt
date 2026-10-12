package com.yaarkhata.app.ui.persondetail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.data.Debt
import com.yaarkhata.app.data.DebtDirection
import com.yaarkhata.app.repository.FinanceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    repository: FinanceRepository,
    onRecordPayment: (Long) -> Unit,
    onBack: () -> Unit
) {
    val debts by repository.getDebtsForPerson(personId).collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by repository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by repository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())
    var personName by remember { mutableStateOf("") }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }
    var showSettleAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(personId) {
        personName = repository.getPersonName(personId)
    }

    val net = debts.sumOf { if (it.direction == DebtDirection.OWED_TO_ME) it.remainingAmount else -it.remainingAmount }
    val hasOpenDebts = debts.any { it.remainingAmount > 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(personName.ifBlank { "Person detail" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(if (net >= 0) "Owes you" else "You owe", style = MaterialTheme.typography.labelMedium)
                    Text("Rs ${"%.0f".format(kotlin.math.abs(net))}", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(Modifier.height(16.dp))
            debts.forEach { debt ->
                val who = if (debt.direction == DebtDirection.OWED_TO_ME) "Owes you" else "You owe"
                val color = if (debt.direction == DebtDirection.OWED_TO_ME) Color(0xFF3B6D11) else Color(0xFFA32D2D)

                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(who, style = MaterialTheme.typography.labelSmall, color = color)
                                Text(debt.description)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rs ${"%.0f".format(debt.remainingAmount)}")
                                IconButton(onClick = { editingDebt = debt }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit debt")
                                }
                            }
                        }
                        Text(
                            "Original Rs ${"%.0f".format(debt.originalAmount)}, ${debt.status.name.lowercase()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (debt.remainingAmount > 0.0) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { onRecordPayment(debt.id) }) { Text("Record payment") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (hasOpenDebts) {
                Button(onClick = { showSettleAll = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Settle all")
                }
                Spacer(Modifier.height(8.dp))
            }

            Button(onClick = {
                scope.launch {
                    val summary = repository.buildWhatsAppSummary(personId, personName.ifBlank { "them" })
                    shareToWhatsApp(context, summary)
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Share via WhatsApp")
            }
        }
    }

    editingDebt?.let { debt ->
        EditDebtDialog(
            debt = debt,
            accounts = accounts,
            categories = categories,
            onDismiss = { editingDebt = null },
            onSave = { newDescription, newAmount, newDirection, newAccountId, newCategoryText ->
                scope.launch {
                    val newCategoryId = newCategoryText?.let { repository.findOrCreateCategory(it) }
                    repository.editDebt(debt.id, newDescription, newAmount, newDirection, newAccountId, newCategoryId)
                    editingDebt = null
                }
            }
        )
    }

    if (showSettleAll) {
        SettleAllDialog(
            accounts = accounts,
            netAmount = kotlin.math.abs(net),
            onDismiss = { showSettleAll = false },
            onConfirm = { splits ->
                scope.launch {
                    repository.settleAllForPerson(personId, splits, note = "Settled all")
                    showSettleAll = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDebtDialog(
    debt: Debt,
    accounts: List<com.yaarkhata.app.data.Account>,
    categories: List<com.yaarkhata.app.data.Category>,
    onDismiss: () -> Unit,
    onSave: (String, Double?, DebtDirection?, Long?, String?) -> Unit
) {
    var description by remember { mutableStateOf(debt.description) }
    var amountText by remember { mutableStateOf("%.0f".format(debt.originalAmount)) }
    var direction by remember { mutableStateOf(debt.direction) }
    var accountId by remember { mutableStateOf(debt.originAccountId) }
    var categoryText by remember {
        mutableStateOf(categories.firstOrNull { it.id == debt.categoryId }?.name ?: "")
    }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Once a payment has been made, amount and direction can't be safely
    // edited - a partial payment already assumes the old ones, and changing
    // either afterward would make that payment inconsistent with the total.
    val editable = debt.remainingAmount == debt.originalAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit debt") },
        text = {
            Column {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                Text("Direction", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = direction == DebtDirection.OWED_TO_ME,
                        onClick = { if (editable) { direction = DebtDirection.OWED_TO_ME; error = null } },
                        enabled = editable,
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Owes me") }
                    SegmentedButton(
                        selected = direction == DebtDirection.I_OWE,
                        onClick = { if (editable) { direction = DebtDirection.I_OWE; error = null } },
                        enabled = editable,
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("I owe") }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Amount") },
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth()
                )

                if (editable && direction == DebtDirection.OWED_TO_ME) {
                    Spacer(Modifier.height(12.dp))
                    Text("From account", style = MaterialTheme.typography.labelMedium)
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
                }

                if (editable && direction == DebtDirection.I_OWE) {
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                        OutlinedTextField(
                            value = categoryText,
                            onValueChange = { categoryText = it; error = null; categoryMenuExpanded = true },
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
                }

                if (!editable) {
                    Text(
                        "Amount, direction, and category can't be changed after a payment has been made against this debt.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!editable) {
                    onSave(description.ifBlank { debt.description }, null, null, null, null)
                    return@TextButton
                }
                val amt = amountText.toDoubleOrNull()
                when {
                    amt == null || amt <= 0.0 -> error = "Enter a valid amount"
                    direction == DebtDirection.OWED_TO_ME && accountId == null -> error = "Choose which account this came from"
                    direction == DebtDirection.I_OWE && categoryText.isBlank() -> error = "Choose a category"
                    else -> {
                        val categoryArg = if (direction == DebtDirection.I_OWE) categoryText.trim() else null
                        onSave(description.ifBlank { debt.description }, amt, direction, accountId, categoryArg)
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleAllDialog(
    accounts: List<com.yaarkhata.app.data.Account>,
    netAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<Long, Double>>) -> Unit
) {
    // Each row is one account's slice of the total settlement - lets the
    // net amount be split across accounts (e.g. partly bank, partly cash).
    val rows = remember { mutableStateListOf(Pair<Long?, String>(null, "")) }
    var error by remember { mutableStateOf<String?>(null) }

    val enteredTotal = rows.sumOf { it.second.toDoubleOrNull() ?: 0.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle all debts") },
        text = {
            Column {
                Text("This settles every open debt at once, Rs ${"%.0f".format(netAmount)} net.")
                Spacer(Modifier.height(12.dp))

                rows.forEachIndexed { index, (accId, amountText) ->
                    Column(Modifier.padding(bottom = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Split ${index + 1}", style = MaterialTheme.typography.labelMedium)
                            if (rows.size > 1) {
                                TextButton(onClick = { rows.removeAt(index) }) { Text("Remove") }
                            }
                        }
                        Row {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = accId == acc.id,
                                    onClick = { rows[index] = acc.id to amountText; error = null },
                                    label = { Text(acc.name) },
                                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { rows[index] = accId to it; error = null },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                TextButton(onClick = { rows.add(null to "") }) { Text("+ Add another account") }

                Text(
                    "Entered so far: Rs ${"%.0f".format(enteredTotal)} of Rs ${"%.0f".format(netAmount)}",
                    style = MaterialTheme.typography.bodySmall
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = rows.map { (accId, text) -> accId to (text.toDoubleOrNull() ?: 0.0) }
                when {
                    parsed.any { it.first == null } -> error = "Choose an account for every split"
                    parsed.any { it.second <= 0.0 } -> error = "Enter a valid amount for every split"
                    kotlin.math.abs(parsed.sumOf { it.second } - netAmount) > 0.5 -> error = "Splits must add up to Rs ${"%.0f".format(netAmount)}"
                    else -> onConfirm(parsed.map { it.first!! to it.second })
                }
            }) { Text("Settle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// Standard Android share intent targeting WhatsApp - no API key or business account needed.
private fun shareToWhatsApp(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        // WhatsApp not installed - fall back to the generic share sheet.
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(fallback, "Share debt summary"))
    }
}
