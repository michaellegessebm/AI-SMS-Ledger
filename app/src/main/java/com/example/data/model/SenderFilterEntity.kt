package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sender_filters")
data class SenderFilterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val phoneNumberOrSenderId: String, // e.g. "+15550123456", "Chase", "Telebirr", "CBEBirr", "MPESA"
    val label: String, // e.g. "Chase Bank Alerts", "Personal Wallet"
    val isEnabled: Boolean = true,
    val autoAiParse: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
