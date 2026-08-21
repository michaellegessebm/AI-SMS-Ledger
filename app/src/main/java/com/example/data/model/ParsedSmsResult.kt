package com.example.data.model

data class ParsedSmsResult(
    val name: String,
    val amount: Double,
    val currency: String = "USD",
    val transactionNumber: String,
    val dateTimeString: String,
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val transactionType: String = "DEBIT", // DEBIT, CREDIT, PAYMENT, TRANSFER, OTHER
    val summary: String = "",
    val confidence: Float = 0.95f
)

data class SmsInboxItem(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val read: Boolean = true,
    val isSelected: Boolean = false
)
