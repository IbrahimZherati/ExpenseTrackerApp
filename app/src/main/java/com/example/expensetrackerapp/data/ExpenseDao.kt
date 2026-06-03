package com.example.expensetrackerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
     fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startOfMonth AND date < :startOfNextMonth ORDER BY date DESC")
    fun getExpensesForMonth(startOfMonth: Long, startOfNextMonth: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalAmount() : Double

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}