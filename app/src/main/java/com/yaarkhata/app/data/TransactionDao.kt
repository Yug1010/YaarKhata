package com.yaarkhata.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getForAccount(accountId: Long): Flow<List<Transaction>>

    // Adjustments and income/expense both feed reports; only category
    // spend (type = EXPENSE) should show up in the Reports screen charts.
    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' ORDER BY date DESC")
    fun getExpenses(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'ADJUSTMENT' ORDER BY date DESC")
    fun getAdjustments(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Update
    suspend fun update(transaction: Transaction)

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Delete
    suspend fun delete(transaction: Transaction)
}
