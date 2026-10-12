package com.yaarkhata.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY name")
    fun getAll(): Flow<List<Person>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: Long): Person?

    @Query("SELECT * FROM people WHERE name LIKE :query || '%' LIMIT 10")
    suspend fun search(query: String): List<Person>

    @Insert
    suspend fun insert(person: Person): Long
}
