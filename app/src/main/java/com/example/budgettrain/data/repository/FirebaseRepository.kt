package com.example.budgettrain.data.repository

import com.example.budgettrain.data.entity.BudgetGoalEntity
import com.example.budgettrain.data.entity.Category
import com.example.budgettrain.data.entity.Expense
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

object FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    // User profile management
    suspend fun updateUserProfile(userId: String, username: String, email: String) {
        val data = hashMapOf<String, Any>(
            "username" to username,
            "email" to email,
            "createdAtMillis" to System.currentTimeMillis()
        )
        db.collection("users").document(userId).set(data).await()
    }

    suspend fun getUserProfile(userId: String): Map<String, Any?>? {
        return try {
            val documentSnapshot = db.collection("users").document(userId).get().await()
            if (documentSnapshot.exists() && documentSnapshot.data != null) {
                documentSnapshot.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Expense operations
    suspend fun addExpense(expense: Expense): String {
        val expenseMap = mapOf(
            "userId" to expense.userId.toString(),
            "categoryId" to expense.categoryId.toString(),
            "amount" to expense.amount,
            "date" to expense.date,
            "startTime" to (expense.startTime?.toString() ?: ""),
            "endTime" to (expense.endTime?.toString() ?: ""),
            "description" to (expense.description ?: ""),
            "imagePath" to (expense.imagePath ?: "")
        )
        val docRef = db.collection("expenses").add(expenseMap).await()
        return docRef.id
    }

    fun getAllExpenses(userId: String? = null): Flow<List<Expense>> = callbackFlow {
        val query = if (userId != null) {
            db.collection("expenses")
                .whereEqualTo("userId", userId.toString())
                .orderBy("date", Query.Direction.DESCENDING)
        } else {
            db.collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val expenses = snapshot?.documents?.map { doc ->
                Expense(
                    id = doc.id.hashCode().toLong(),
                    userId = (doc.get("userId") as? String)?.toLongOrNull() ?: 0L,
                    categoryId = (doc.get("categoryId") as? String)?.toLongOrNull() ?: 0L,
                    amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
                    date = (doc.get("date") as? Number)?.toLong() ?: 0L,
                    startTime = (doc.get("startTime") as? String)?.takeIf { it.isNotEmpty() }?.toLongOrNull(),
                    endTime = (doc.get("endTime") as? String)?.takeIf { it.isNotEmpty() }?.toLongOrNull(),
                    description = doc.get("description") as? String,
                    imagePath = doc.get("imagePath") as? String
                )
            } ?: emptyList()
            
            trySend(expenses)
        }
        
        awaitClose { registration.remove() }
    }

    fun getExpensesInRange(userId: String?, start: Long, end: Long): Flow<List<Expense>> = callbackFlow {
        val query = if (userId != null) {
            db.collection("expenses")
                .whereEqualTo("userId", userId.toString())
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .orderBy("date", Query.Direction.DESCENDING)
        } else {
            db.collection("expenses")
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .orderBy("date", Query.Direction.DESCENDING)
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val expenses = snapshot?.documents?.map { doc ->
                Expense(
                    id = doc.id.hashCode().toLong(),
                    userId = (doc.get("userId") as? String)?.toLongOrNull() ?: 0L,
                    categoryId = (doc.get("categoryId") as? String)?.toLongOrNull() ?: 0L,
                    amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
                    date = (doc.get("date") as? Number)?.toLong() ?: 0L,
                    startTime = (doc.get("startTime") as? String)?.takeIf { it.isNotEmpty() }?.toLongOrNull(),
                    endTime = (doc.get("endTime") as? String)?.takeIf { it.isNotEmpty() }?.toLongOrNull(),
                    description = doc.get("description") as? String,
                    imagePath = doc.get("imagePath") as? String
                )
            } ?: emptyList()
            
            trySend(expenses)
        }
        
        awaitClose { registration.remove() }
    }

    suspend fun deleteExpense(expenseId: String) {
        db.collection("expenses").document(expenseId).delete().await()
    }

    fun getExpensesWithCategory(userId: String? = null): Flow<List<ExpenseWithCategory>> {
        return combine(
            getAllExpenses(userId),
            getAllCategories(userId)
        ) { expenses, categories ->
            val categoryMap = categories.associateBy { it.id.toString() }
            expenses.map { expense ->
                val category = categoryMap[expense.categoryId.toString()]
                ExpenseWithCategory(
                    id = expense.id,
                    userId = expense.userId,
                    categoryId = expense.categoryId,
                    amount = expense.amount,
                    date = expense.date,
                    startTime = expense.startTime,
                    endTime = expense.endTime,
                    description = expense.description,
                    imagePath = expense.imagePath,
                    categoryName = category?.name ?: "Uncategorized"
                )
            }
        }
    }

    fun getCategoryTotals(userId: String?, start: Long, end: Long): Flow<List<CategoryTotal>> {
        return combine(
            getExpensesInRange(userId, start, end),
            getAllCategories(userId)
        ) { expenses, categories ->
            val categoryMap = categories.associateBy { it.id.toString() }
            val totalsByCategory = expenses.groupBy { it.categoryId.toString() }
                .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
            
            totalsByCategory.map { (categoryId, total) ->
                val category = categoryMap[categoryId]
                CategoryTotal(
                    categoryName = category?.name ?: "Uncategorized",
                    total = total
                )
            }.sortedByDescending { it.total }
        }
    }

    // Category operations
    suspend fun addCategory(category: Category): String {
        val categoryMap = mapOf(
            "userId" to category.userId.toString(),
            "name" to category.name,
            "color" to (category.color ?: 0xFF607D8B)
        )
        val docRef = db.collection("categories").add(categoryMap).await()
        return docRef.id
    }

    suspend fun updateCategory(categoryId: String, category: Category) {
        val categoryMap = mapOf(
            "userId" to category.userId.toString(),
            "name" to category.name,
            "color" to (category.color ?: 0xFF607D8B)
        )
        db.collection("categories").document(categoryId).set(categoryMap).await()
    }

    fun getAllCategories(userId: String? = null): Flow<List<Category>> = callbackFlow {
        val query = if (userId != null) {
            db.collection("categories")
                .whereEqualTo("userId", userId.toString())
                .orderBy("name")
        } else {
            db.collection("categories")
                .orderBy("name")
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val categories = snapshot?.documents?.map { doc ->
                Category(
                    id = doc.id.hashCode().toLong(),
                    userId = (doc.get("userId") as? String)?.toLongOrNull() ?: 0L,
                    name = doc.get("name") as? String ?: "",
                    color = (doc.get("color") as? Number)?.toLong()
                )
            } ?: emptyList()
            
            trySend(categories)
        }
        
        awaitClose { registration.remove() }
    }

    suspend fun getCategoryByName(userId: String?, name: String): Category? {
        return try {
            val query = if (userId != null) {
                db.collection("categories")
                    .whereEqualTo("userId", userId.toString())
                    .whereEqualTo("name", name)
                    .limit(1)
            } else {
                db.collection("categories")
                    .whereEqualTo("name", name)
                    .limit(1)
            }
            
            val snapshot = query.get().await()
            snapshot.documents.firstOrNull()?.let { doc ->
                Category(
                    id = doc.id.hashCode().toLong(),
                    userId = (doc.get("userId") as? String)?.toLongOrNull() ?: 0L,
                    name = doc.get("name") as? String ?: "",
                    color = (doc.get("color") as? Number)?.toLong()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    // Budget Goal operations
    suspend fun addBudgetGoal(goal: BudgetGoalEntity): String {
        val goalMap = mapOf(
            "userId" to goal.userId.toString(),
            "monthKey" to goal.monthKey,
            "minimumGoal" to goal.minimumGoal,
            "maximumGoal" to goal.maximumGoal
        )
        val docRef = db.collection("budget_goals").add(goalMap).await()
        return docRef.id
    }

    suspend fun updateBudgetGoal(goalId: String, goal: BudgetGoalEntity) {
        val goalMap = mapOf(
            "userId" to goal.userId.toString(),
            "monthKey" to goal.monthKey,
            "minimumGoal" to goal.minimumGoal,
            "maximumGoal" to goal.maximumGoal
        )
        db.collection("budget_goals").document(goalId).set(goalMap).await()
    }

    fun getBudgetGoal(userId: String?, monthKey: String): Flow<BudgetGoalEntity?> = callbackFlow {
        val query = if (userId != null) {
            db.collection("budget_goals")
                .whereEqualTo("userId", userId.toString())
                .whereEqualTo("monthKey", monthKey)
                .limit(1)
        } else {
            db.collection("budget_goals")
                .whereEqualTo("monthKey", monthKey)
                .limit(1)
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            
            val goal = snapshot?.documents?.firstOrNull()?.let { doc ->
                BudgetGoalEntity(
                    id = doc.id.hashCode().toLong(),
                    userId = (doc.get("userId") as? String)?.toLongOrNull() ?: 0L,
                    monthKey = doc.get("monthKey") as? String ?: "",
                    minimumGoal = (doc.get("minimumGoal") as? Number)?.toDouble() ?: 0.0,
                    maximumGoal = (doc.get("maximumGoal") as? Number)?.toDouble() ?: 0.0
                )
            }
            trySend(goal)
        }
        
        awaitClose { registration.remove() }
    }

    // Data classes for compatibility
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

    data class CategoryTotal(
        val categoryName: String,
        val total: Double
    )
}
