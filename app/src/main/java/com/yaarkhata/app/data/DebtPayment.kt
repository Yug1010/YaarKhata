package com.yaarkhata.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "debt_payments")
data class DebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    // Chosen at settlement time, independent of how the debt was created -
    // the debt might have started from a shared bill with no account
    // involved, but the actual repayment always lands in a real account.
    val accountId: Long,
    val amount: Double,
    val date: Date,
    val note: String? = null
)
