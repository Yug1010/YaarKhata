package com.yaarkhata.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType { BANK, CASH }

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double
)
