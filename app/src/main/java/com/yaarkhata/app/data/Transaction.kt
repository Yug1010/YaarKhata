package com.yaarkhata.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class TransactionType { INCOME, EXPENSE, ADJUSTMENT }

// Debts are intentionally NOT recorded here - they live in the Debt table
// so lending/borrowing never pollutes expense reports.
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long? = null,
    val date: Date,
    val note: String? = null,
    // Only meaningful when type == ADJUSTMENT: true adds to the balance
    // (e.g. dad sends money), false subtracts (e.g. a mutual fund autopay)
    // without ever counting as a category expense.
    val isAddition: Boolean = true
)
