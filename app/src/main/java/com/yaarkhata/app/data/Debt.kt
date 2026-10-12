package com.yaarkhata.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class DebtDirection { OWED_TO_ME, I_OWE }
enum class DebtStatus { PENDING, PARTIAL, SETTLED }

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val originalAmount: Double,
    val remainingAmount: Double,
    val direction: DebtDirection,
    val date: Date,
    val description: String,
    val status: DebtStatus = DebtStatus.PENDING,
    // Only set for OWED_TO_ME debts (you fronted the money) - remembered so
    // editing the amount later can correct the right account's balance.
    val originAccountId: Long? = null,
    // Only set for I_OWE debts - chosen at creation, same as a normal
    // expense, since settling this debt later is real spending that belongs
    // under its own category (groceries, travel, etc.) rather than a generic
    // "debt repayment" bucket.
    val categoryId: Long? = null
)
