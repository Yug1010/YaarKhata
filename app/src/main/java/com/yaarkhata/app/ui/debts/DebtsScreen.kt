package com.yaarkhata.app.ui.debts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarkhata.app.data.DebtDirection
import com.yaarkhata.app.repository.FinanceRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    repository: FinanceRepository,
    onAddDebt: () -> Unit,
    onOpenPerson: (Long) -> Unit
) {
    val people by repository.getPeople().collectAsStateWithLifecycle(initialValue = emptyList())
    val debts by repository.getDebts().collectAsStateWithLifecycle(initialValue = emptyList())

    // Net balance per person: positive = they owe you, negative = you owe them.
    val netByPerson = debts.groupBy { it.personId }.mapValues { (_, list) ->
        list.sumOf { d -> if (d.direction == DebtDirection.OWED_TO_ME) d.remainingAmount else -d.remainingAmount }
    }

    // Only people with at least one debt record show up here at all -
    // net == 0 means settled, not "never had a debt".
    val peopleWithHistory = people.filter { netByPerson.containsKey(it.id) }
    val active = peopleWithHistory.filter { (netByPerson[it.id] ?: 0.0) != 0.0 }
    val settled = peopleWithHistory.filter { (netByPerson[it.id] ?: 0.0) == 0.0 }

    Scaffold(
        floatingActionButton = {
            // Bottom-right, the standard reachable spot for a one-handed thumb grip.
            FloatingActionButton(onClick = onAddDebt) {
                Icon(Icons.Filled.Add, contentDescription = "Add debt")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Debts", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            active.forEach { person ->
                val net = netByPerson[person.id] ?: 0.0
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenPerson(person.id) }
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(person.name)
                        val label = if (net > 0) "Owes you Rs ${"%.0f".format(net)}" else "You owe Rs ${"%.0f".format(-net)}"
                        val color = if (net > 0) Color(0xFF3B6D11) else Color(0xFFA32D2D)
                        Text(label, color = color)
                    }
                }
            }

            if (settled.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Settled up", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                settled.forEach { person ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenPerson(person.id) }
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(person.name)
                            Text("Settled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            // Leave room so the last card isn't hidden behind the FAB.
            Spacer(Modifier.height(72.dp))
        }
    }
}
