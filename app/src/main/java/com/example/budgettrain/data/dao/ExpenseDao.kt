package com.example.budgettrain.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.budgettrain.data.entity.Expense
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(val categoryName: String, val total: Double)
data class ExpenseWithCategory(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val amount: Double,
    val date: Long,
    val startTime: Long?,
    val endTime: Long?,
    val description: String?,
    val imagePath: String?,
    val categoryName: String
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getExpensesInRange(start: Long, end: Long): Flow<List<Expense>>

    @Query(
        """
        SELECT c.name AS categoryName, SUM(e.amount) AS total
        FROM expenses e 
        JOIN categories c ON e.categoryId = c.id 
        WHERE e.date BETWEEN :start AND :end 
        GROUP BY c.id
        ORDER BY total DESC
        """
    )
    fun getCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Query(
        """
        SELECT e.id, e.userId, e.categoryId, e.amount, e.date, e.startTime, e.endTime, e.description, e.imagePath,
               COALESCE(c.name, 'Uncategorized') AS categoryName
        FROM expenses e
        LEFT JOIN categories c ON e.categoryId = c.id
        ORDER BY e.date DESC
        """
    )
    fun getAllExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}


