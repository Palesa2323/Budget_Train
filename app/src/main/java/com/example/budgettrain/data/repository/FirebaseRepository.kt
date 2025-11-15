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
        try {
            val data = hashMapOf<String, Any>(
                "username" to username,
                "email" to email,
                "createdAtMillis" to System.currentTimeMillis()
            )
            android.util.Log.d("FirebaseRepository", "Saving user profile: userId=$userId, username=$username, email=$email")
            db.collection("users").document(userId).set(data).await()
            android.util.Log.d("FirebaseRepository", "User profile saved successfully to Firestore")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error saving user profile: ${e.message}", e)
            throw e
        }
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
        try {
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
            android.util.Log.d("FirebaseRepository", "Saving expense: userId=${expense.userId} (as string: ${expense.userId.toString()}), amount=${expense.amount}, description=${expense.description}, categoryId=${expense.categoryId}")
            val docRef = db.collection("expenses").add(expenseMap).await()
            android.util.Log.d("FirebaseRepository", "Expense saved successfully with document ID: ${docRef.id}, userId in document: ${expenseMap["userId"]}")
            return docRef.id
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error saving expense: ${e.message}", e)
            throw e
        }
    }

    fun getAllExpenses(userId: String? = null): Flow<List<Expense>> = callbackFlow {
        val query = if (userId != null) {
            // Convert Firebase UID string to Long using hashCode for consistency with how we save
            val userIdLong = userId.hashCode().toLong()
            android.util.Log.d("FirebaseRepository", "Querying expenses for userId: $userId (converted to Long: $userIdLong)")
            db.collection("expenses")
                .whereEqualTo("userId", userIdLong.toString())
                .orderBy("date", Query.Direction.DESCENDING)
        } else {
            android.util.Log.d("FirebaseRepository", "Querying all expenses (no userId filter)")
            db.collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseRepository", "Error getting expenses: ${error.message}", error)
                android.util.Log.e("FirebaseRepository", "Error code: ${error.code}, details: ${error.localizedMessage}")
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("FirebaseRepository", "PERMISSION_DENIED: Check Firestore security rules")
                } else if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    android.util.Log.e("FirebaseRepository", "FAILED_PRECONDITION: Missing composite index. Check Firebase Console for index creation link.")
                }
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
            
            android.util.Log.d("FirebaseRepository", "Retrieved ${expenses.size} expenses for userId: $userId")
            trySend(expenses)
        }
        
        awaitClose { registration.remove() }
    }

    fun getExpensesInRange(userId: String?, start: Long, end: Long): Flow<List<Expense>> = callbackFlow {
        val query = if (userId != null) {
            // Convert Firebase UID string to Long using hashCode for consistency with how we save
            val userIdLong = userId.hashCode().toLong()
            db.collection("expenses")
                .whereEqualTo("userId", userIdLong.toString())
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
                android.util.Log.e("FirebaseRepository", "Error getting expenses in range: ${error.message}", error)
                android.util.Log.e("FirebaseRepository", "Error code: ${error.code}, details: ${error.localizedMessage}")
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("FirebaseRepository", "PERMISSION_DENIED: Check Firestore security rules")
                } else if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    android.util.Log.e("FirebaseRepository", "FAILED_PRECONDITION: Missing composite index. Check Firebase Console for index creation link.")
                }
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
        android.util.Log.d("FirebaseRepository", "Saving category: name=${category.name}, userId=${category.userId}")
        val docRef = db.collection("categories").add(categoryMap).await()
        android.util.Log.d("FirebaseRepository", "Category saved successfully with ID: ${docRef.id}")
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
            // Convert Firebase UID string to Long using hashCode for consistency with how we save
            val userIdLong = userId.hashCode().toLong()
            db.collection("categories")
                .whereEqualTo("userId", userIdLong.toString())
                .orderBy("name")
        } else {
            db.collection("categories")
                .orderBy("name")
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseRepository", "Error getting categories: ${error.message}", error)
                android.util.Log.e("FirebaseRepository", "Error code: ${error.code}, details: ${error.localizedMessage}")
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("FirebaseRepository", "PERMISSION_DENIED: Check Firestore security rules")
                } else if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    android.util.Log.e("FirebaseRepository", "FAILED_PRECONDITION: Missing composite index. Check Firebase Console for index creation link.")
                }
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
                // Convert Firebase UID string to Long using hashCode for consistency with how we save
                val userIdLong = userId.hashCode().toLong()
                db.collection("categories")
                    .whereEqualTo("userId", userIdLong.toString())
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
        android.util.Log.d("FirebaseRepository", "Saving budget goal: monthKey=${goal.monthKey}, min=${goal.minimumGoal}, max=${goal.maximumGoal}")
        val docRef = db.collection("budget_goals").add(goalMap).await()
        android.util.Log.d("FirebaseRepository", "Budget goal saved successfully with ID: ${docRef.id}")
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
            // Convert Firebase UID string to Long using hashCode for consistency with how we save
            val userIdLong = userId.hashCode().toLong()
            db.collection("budget_goals")
                .whereEqualTo("userId", userIdLong.toString())
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
