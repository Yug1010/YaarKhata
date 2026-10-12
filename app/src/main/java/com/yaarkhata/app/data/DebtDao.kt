package com.yaarkhata.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY date DESC")
    fun getAll(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE personId = :personId ORDER BY date DESC")
    fun getForPerson(personId: Long): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): Debt?

    @Insert
    suspend fun insert(debt: Debt): Long

    @Update
    suspend fun update(debt: Debt)

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY date DESC")
    fun getPayments(debtId: Long): Flow<List<DebtPayment>>

    @Query("SELECT * FROM debt_payments")
    fun getAllPayments(): Flow<List<DebtPayment>>

    @Insert
    suspend fun insertPayment(payment: DebtPayment): Long
}
