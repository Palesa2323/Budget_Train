package com.example.budgettrain.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseStorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    suspend fun uploadReceiptImage(userId: String, localFileUri: Uri): Result<String> {
        return try {
            val fileName = "receipts/${userId}/${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(fileName)
            val uploadTask = imageRef.putFile(localFileUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadReceiptImageFromFile(userId: String, file: File): Result<String> {
        return try {
            val fileName = "receipts/${userId}/${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(fileName)
            val uploadTask = imageRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imagePath: String) {
        try {
            if (imagePath.startsWith("gs://") || imagePath.contains("firebasestorage")) {
                val imageRef = storage.getReferenceFromUrl(imagePath)
                imageRef.delete().await()
            }
        } catch (e: Exception) {
            // Ignore deletion errors
        }
    }
}

