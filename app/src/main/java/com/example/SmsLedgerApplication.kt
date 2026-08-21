package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.repository.TransactionRepository

class SmsLedgerApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        TransactionRepository(
            database.transactionDao(),
            database.senderFilterDao(),
            database.remoteDatabaseConfigDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SMS AI Parser Alerts"
            val descriptionText = "Notifications when incoming SMS transactions are automatically extracted"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sms_ai_ledger_channel"
        lateinit var instance: SmsLedgerApplication
            private set
    }
}
