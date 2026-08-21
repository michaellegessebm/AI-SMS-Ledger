package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val senderNumber: String,
    val merchantOrName: String,
    val amount: Double,
    val currency: String = "USD",
    val transactionNumber: String,
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val dateTimeString: String = "",
    val transactionType: String = "DEBIT", // DEBIT, CREDIT, PAYMENT, TRANSFER, OTHER
    val rawMessageBody: String,
    val aiSummary: String = "",
    val isAutoTriggered: Boolean = true,
    val remoteSyncStatus: String = "PENDING", // PENDING, SYNCED, FAILED, LOCAL_ONLY
    val remoteSyncError: String? = null,
    val remoteSyncTime: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
