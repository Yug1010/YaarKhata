package com.yaarkhata.app.repository

import com.yaarkhata.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepository(private val db: AppDatabase) {

    // --- Accounts ---
    fun getAccounts() = db.accountDao().getAll()
    fun getNetWorth() = db.accountDao().getNetWorth()
    suspend fun addAccount(name: String, type: AccountType, openingBalance: Double) =
        db.accountDao().insert(Account(name = name, type = type, balance = openingBalance))
    suspend fun deleteAccount(account: Account) = db.accountDao().delete(account)

    // Wipes every table - used by the hidden "reset all data" option, and
    // matches what a fresh install should look like.
    suspend fun resetAllData() = withContext(Dispatchers.IO) { db.clearAllTables() }

    // --- Backup / restore ---
    // A full export is the only way data survives a lost or replaced phone,
    // since everything else lives purely in this device's local database
    // (no cloud sync, by design, for privacy). The caller is responsible for
    // writing the returned string to a file the user picks.
    suspend fun exportDataAsJson(): String {
        val accounts = db.accountDao().getAll().first()
        val categories = db.categoryDao().getAll().first()
        val people = db.personDao().getAll().first()
        val transactions = db.transactionDao().getAll().first()
        val debts = db.debtDao().getAll().first()
        val payments = db.debtDao().getAllPayments().first()

        val root = JSONObject()
        root.put("version", 1)

        root.put("accounts", JSONArray().apply {
            accounts.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("type", it.type.name); put("balance", it.balance)
                })
            }
        })
        root.put("categories", JSONArray().apply {
            categories.forEach {
                put(JSONObject().apply { put("id", it.id); put("name", it.name); put("icon", it.icon) })
            }
        })
        root.put("people", JSONArray().apply {
            people.forEach {
                put(JSONObject().apply { put("id", it.id); put("name", it.name); put("phone", it.phone ?: JSONObject.NULL) })
            }
        })
        root.put("transactions", JSONArray().apply {
            transactions.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("accountId", it.accountId); put("amount", it.amount)
                    put("type", it.type.name); put("categoryId", it.categoryId ?: JSONObject.NULL)
                    put("date", it.date.time); put("note", it.note ?: JSONObject.NULL)
                    put("isAddition", it.isAddition)
                })
            }
        })
        root.put("debts", JSONArray().apply {
            debts.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("personId", it.personId); put("originalAmount", it.originalAmount)
                    put("remainingAmount", it.remainingAmount); put("direction", it.direction.name)
                    put("date", it.date.time); put("description", it.description); put("status", it.status.name)
                    put("originAccountId", it.originAccountId ?: JSONObject.NULL)
                    put("categoryId", it.categoryId ?: JSONObject.NULL)
                })
            }
        })
        root.put("debtPayments", JSONArray().apply {
            payments.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("debtId", it.debtId); put("accountId", it.accountId)
                    put("amount", it.amount); put("date", it.date.time); put("note", it.note ?: JSONObject.NULL)
                })
            }
        })
        return root.toString()
    }

    // Replaces everything currently in the app with what's in the backup.
    // IDs are preserved on purpose - Room treats a non-zero id on an
    // autoGenerate primary key as "use this exact id", which keeps every
    // foreign key reference (accountId, personId, debtId, etc.) intact
    // across the restore. Tables are cleared first, then inserted in
    // dependency order (accounts/categories/people before anything that
    // references them, debts before payments).
    suspend fun importDataFromJson(json: String) {
        val root = JSONObject(json)
        withContext(Dispatchers.IO) { db.clearAllTables() }

        root.optJSONArray("accounts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.accountDao().insert(
                    Account(id = o.getLong("id"), name = o.getString("name"), type = AccountType.valueOf(o.getString("type")), balance = o.getDouble("balance"))
                )
            }
        }
        root.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.categoryDao().insert(Category(id = o.getLong("id"), name = o.getString("name"), icon = o.getString("icon")))
            }
        }
        root.optJSONArray("people")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.personDao().insert(Person(id = o.getLong("id"), name = o.getString("name"), phone = if (!o.has("phone") || o.isNull("phone")) null else o.getString("phone")))
            }
        }
        root.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.transactionDao().insert(
                    Transaction(
                        id = o.getLong("id"),
                        accountId = o.getLong("accountId"),
                        amount = o.getDouble("amount"),
                        type = TransactionType.valueOf(o.getString("type")),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        date = Date(o.getLong("date")),
                        note = if (o.isNull("note")) null else o.getString("note"),
                        isAddition = o.getBoolean("isAddition")
                    )
                )
            }
        }
        root.optJSONArray("debts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.debtDao().insert(
                    Debt(
                        id = o.getLong("id"),
                        personId = o.getLong("personId"),
                        originalAmount = o.getDouble("originalAmount"),
                        remainingAmount = o.getDouble("remainingAmount"),
                        direction = DebtDirection.valueOf(o.getString("direction")),
                        date = Date(o.getLong("date")),
                        description = o.getString("description"),
                        status = DebtStatus.valueOf(o.getString("status")),
                        originAccountId = if (o.isNull("originAccountId")) null else o.getLong("originAccountId"),
                        categoryId = if (!o.has("categoryId") || o.isNull("categoryId")) null else o.getLong("categoryId")
                    )
                )
            }
        }
        root.optJSONArray("debtPayments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.debtDao().insertPayment(
                    DebtPayment(
                        id = o.getLong("id"),
                        debtId = o.getLong("debtId"),
                        accountId = o.getLong("accountId"),
                        amount = o.getDouble("amount"),
                        date = Date(o.getLong("date")),
                        note = if (o.isNull("note")) null else o.getString("note")
                    )
                )
            }
        }
    }

    // --- Transactions (expense / income / adjustment only, never debts) ---
    fun getExpenses() = db.transactionDao().getExpenses()
    fun getAdjustments() = db.transactionDao().getAdjustments()
    fun getTransactionsForAccount(accountId: Long) = db.transactionDao().getForAccount(accountId)

    // --- Categories ---
    fun getCategories() = db.categoryDao().getAll()
    suspend fun findOrCreateCategory(name: String, icon: String = "\uD83C\uDFF7\uFE0F"): Long {
        db.categoryDao().findByName(name)?.let { return it.id }
        return db.categoryDao().insert(Category(name = name, icon = icon))
    }

    suspend fun addTransaction(
        accountId: Long,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        note: String?,
        date: Date = Date(),
        // Only used for ADJUSTMENT: true = money added (e.g. dad sends cash),
        // false = money subtracted (e.g. a mutual fund autopay) - either way
        // it stays out of the Transaction rows that Reports reads as spending.
        isAddition: Boolean = true
    ) {
        db.transactionDao().insert(
            Transaction(
                accountId = accountId,
                amount = amount,
                type = type,
                categoryId = categoryId,
                date = date,
                note = note,
                isAddition = isAddition
            )
        )
        val delta = when (type) {
            TransactionType.EXPENSE -> -amount
            TransactionType.INCOME -> amount
            TransactionType.ADJUSTMENT -> if (isAddition) amount else -amount
        }
        db.accountDao().adjustBalance(accountId, delta)
    }

    suspend fun getTransactionById(id: Long) = db.transactionDao().getById(id)

    // Edits an existing transaction's amount, account, category, or note.
    // Type and isAddition are left as they were - this isn't meant to turn
    // an expense into income, just correct a mistake in the same entry.
    // Reverses the old balance effect on the old account, then reapplies
    // the new one on the (possibly different) new account.
    suspend fun editTransaction(
        transactionId: Long,
        newAmount: Double,
        newAccountId: Long,
        newCategoryId: Long?,
        newNote: String?
    ) {
        val old = db.transactionDao().getById(transactionId) ?: return

        fun deltaFor(amount: Double) = when (old.type) {
            TransactionType.EXPENSE -> -amount
            TransactionType.INCOME -> amount
            TransactionType.ADJUSTMENT -> if (old.isAddition) amount else -amount
        }

        db.accountDao().adjustBalance(old.accountId, -deltaFor(old.amount))
        db.accountDao().adjustBalance(newAccountId, deltaFor(newAmount))

        db.transactionDao().update(
            old.copy(amount = newAmount, accountId = newAccountId, categoryId = newCategoryId, note = newNote)
        )
    }

    // --- People ---
    fun getPeople() = db.personDao().getAll()

    suspend fun getPersonName(personId: Long): String = db.personDao().getById(personId)?.name ?: ""

    suspend fun findOrCreatePerson(name: String): Long {
        db.personDao().search(name).firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        return db.personDao().insert(Person(name = name))
    }

    // --- Debts (kept entirely separate from expense tracking) ---
    fun getDebts() = db.debtDao().getAll()
    fun getDebtsForPerson(personId: Long) = db.debtDao().getForPerson(personId)
    fun getPayments(debtId: Long) = db.debtDao().getPayments(debtId)
    suspend fun getDebtById(debtId: Long) = db.debtDao().getById(debtId)

    suspend fun addDebt(
        personId: Long,
        accountId: Long?,
        categoryId: Long?,
        amount: Double,
        direction: DebtDirection,
        description: String,
        date: Date = Date()
    ) {
        db.debtDao().insert(
            Debt(
                personId = personId,
                originalAmount = amount,
                remainingAmount = amount,
                direction = direction,
                date = date,
                description = description,
                originAccountId = if (direction == DebtDirection.OWED_TO_ME) accountId else null,
                categoryId = if (direction == DebtDirection.I_OWE) categoryId else null
            )
        )
        // OWED_TO_ME means you fronted the money right now (paid a friend's
        // share, or the whole bill) - that's real cash leaving your account
        // immediately, so it's deducted here.
        // I_OWE means someone else paid on your behalf - nothing has left
        // your pocket yet, so no account is touched until you settle up.
        if (direction == DebtDirection.OWED_TO_ME && accountId != null) {
            db.accountDao().adjustBalance(accountId, -amount)
        }
    }

    // Reverses whatever balance effect the ORIGINAL entry had, then reapplies
    // the new one based on the (possibly changed) direction/amount. This
    // covers every edit case uniformly - amount change, direction flip, or
    // both - without needing separate logic per transition.
    // Only allowed while no payment has been made yet (remainingAmount ==
    // originalAmount), since a partial payment already assumes the old
    // amount and direction; changing either afterward would make the
    // payment history inconsistent. Description can always be edited.
    suspend fun editDebt(
        debtId: Long,
        newDescription: String,
        newAmount: Double?,
        newDirection: DebtDirection?,
        newAccountId: Long?,
        newCategoryId: Long?
    ) {
        val debt = db.debtDao().getById(debtId) ?: return
        val paid = debt.originalAmount - debt.remainingAmount

        if (paid != 0.0) {
            // Payment already made - only the description is safe to change.
            db.debtDao().update(debt.copy(description = newDescription))
            return
        }

        val finalDirection = newDirection ?: debt.direction
        val finalAmount = newAmount ?: debt.originalAmount

        // Step 1: undo the original creation-time balance effect, if any.
        if (debt.direction == DebtDirection.OWED_TO_ME && debt.originAccountId != null) {
            db.accountDao().adjustBalance(debt.originAccountId, debt.originalAmount)
        }

        // Step 2: apply the balance effect for the new direction/amount.
        var finalOriginAccountId: Long? = null
        if (finalDirection == DebtDirection.OWED_TO_ME) {
            val accId = newAccountId ?: debt.originAccountId
            if (accId != null) {
                db.accountDao().adjustBalance(accId, -finalAmount)
                finalOriginAccountId = accId
            }
        }

        // Category only matters for I_OWE (it's what the eventual repayment
        // expense gets tagged under) - clear it if direction moved away from I_OWE.
        val finalCategoryId = if (finalDirection == DebtDirection.I_OWE) (newCategoryId ?: debt.categoryId) else null

        db.debtDao().update(
            debt.copy(
                description = newDescription,
                originalAmount = finalAmount,
                remainingAmount = finalAmount,
                direction = finalDirection,
                originAccountId = finalOriginAccountId,
                categoryId = finalCategoryId,
                status = DebtStatus.PENDING
            )
        )
    }

    suspend fun recordPayment(debtId: Long, accountId: Long, amount: Double, note: String?, date: Date = Date()) {
        val debt = db.debtDao().getById(debtId) ?: return
        val newRemaining = (debt.remainingAmount - amount).coerceAtLeast(0.0)
        val newStatus = when {
            newRemaining <= 0.0 -> DebtStatus.SETTLED
            newRemaining < debt.originalAmount -> DebtStatus.PARTIAL
            else -> DebtStatus.PENDING
        }
        db.debtDao().update(debt.copy(remainingAmount = newRemaining, status = newStatus))
        db.debtDao().insertPayment(DebtPayment(debtId = debtId, accountId = accountId, amount = amount, date = date, note = note))

        // A payment is the actual moment cash changes hands: if they owed you
        // and pay you back, your chosen account goes up; if you owed them and
        // you pay, it goes down. The account can be different from whatever
        // was in play when the debt itself was first logged.
        val delta = if (debt.direction == DebtDirection.OWED_TO_ME) amount else -amount
        db.accountDao().adjustBalance(accountId, delta)

        // I_OWE settlements are real spending that was never recorded
        // anywhere else: someone else fronted the money, so no Expense row
        // was created at the time, and this payment is the only moment real
        // cash actually leaves you for it. Log it as an expense under the
        // category chosen when the debt was created, so it reads exactly
        // like a normal expense entry rather than a generic "debt" bucket.
        // OWED_TO_ME repayments are the opposite direction (money coming
        // back to you) and are intentionally NOT logged here - they're a
        // reimbursement, not new spending, and the original expense (if any)
        // was already your own responsibility to record via Add entry.
        if (debt.direction == DebtDirection.I_OWE) {
            db.transactionDao().insert(
                Transaction(
                    accountId = accountId,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    categoryId = debt.categoryId,
                    date = date,
                    note = note ?: debt.description
                )
            )
        }
    }

    // Settles every open debt with a person in one action - useful once a
    // long back-and-forth list nets down to a single payment. Supports
    // splitting the total across multiple accounts (e.g. part of it landed
    // in your bank, the rest in cash). Payments are drained in order against
    // the oldest open debts first, and recordPayment is still called per
    // debt/account slice so each keeps its own payment history entry.
    suspend fun settleAllForPerson(personId: Long, payments: List<Pair<Long, Double>>, note: String?) {
        val openDebts = db.debtDao().getForPerson(personId).first()
            .filter { it.remainingAmount > 0.0 }
            .sortedBy { it.date }
        val remaining = payments.map { it.first to it.second }.toMutableList()

        for (debt in openDebts) {
            var debtRemaining = debt.remainingAmount
            while (debtRemaining > 0.0 && remaining.isNotEmpty()) {
                val (accountId, available) = remaining.first()
                val use = minOf(available, debtRemaining)
                if (use > 0.0) {
                    recordPayment(debt.id, accountId, use, note)
                }
                debtRemaining -= use
                val leftover = available - use
                if (leftover <= 0.0) remaining.removeAt(0) else remaining[0] = accountId to leftover
            }
            if (remaining.isEmpty()) break
        }
    }

    // --- WhatsApp share text ---
    // Formats a person's open debt history for Intent.ACTION_SEND. Settled
    // debts are left out entirely - they're no longer actionable, so
    // including them just clutters the message.
    suspend fun buildWhatsAppSummary(personId: Long, personName: String): String {
        val debts = db.debtDao().getForPerson(personId).first()
        val open = debts.filter { it.status != DebtStatus.SETTLED }
        if (open.isEmpty()) return "No pending debts with $personName."

        val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
        val net = open.sumOf { d -> if (d.direction == DebtDirection.OWED_TO_ME) d.remainingAmount else -d.remainingAmount }
        val netLine = when {
            net > 0 -> "Net: $personName owes you Rs ${"%.0f".format(net)}"
            net < 0 -> "Net: you owe $personName Rs ${"%.0f".format(-net)}"
            else -> "Net: settled up"
        }

        fun formatDebt(d: Debt): String {
            val who = if (d.direction == DebtDirection.OWED_TO_ME) "Owes you" else "You owe"
            val paid = d.originalAmount - d.remainingAmount
            val paidNote = if (paid > 0) " (paid Rs ${"%.0f".format(paid)} of Rs ${"%.0f".format(d.originalAmount)})" else ""
            return "- $who Rs ${"%.0f".format(d.remainingAmount)} - ${d.description}, ${fmt.format(d.date)}$paidNote"
        }

        val sb = StringBuilder()
        sb.append("Debt summary with $personName\n")
        sb.append(netLine).append("\n\n")
        open.forEach { sb.append(formatDebt(it)).append("\n") }
        return sb.toString().trim()
    }
}
