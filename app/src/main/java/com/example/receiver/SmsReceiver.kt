package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.ai.GeminiSmsParser
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.remote.GameWebhookClient
import com.example.data.remote.RemoteDatabaseSyncManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            intent.action != "android.provider.Telephony.SMS_RECEIVED") {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: "Unknown"
        val fullBodyBuilder = StringBuilder()
        var timestampMillis = System.currentTimeMillis()

        for (sms in messages) {
            fullBodyBuilder.append(sms.messageBody ?: "")
            if (sms.timestampMillis > 0) {
                timestampMillis = sms.timestampMillis
            }
        }
        val fullBody = fullBodyBuilder.toString()

        Log.d(TAG, "Incoming SMS received from: $sender, body length: ${fullBody.length}")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processIncomingSms(context, sender, fullBody, timestampMillis)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processIncomingSms(
        context: Context,
        sender: String,
        body: String,
        timestampMillis: Long
    ) {
        val database = AppDatabase.getDatabase(context)
        val filterDao = database.senderFilterDao()
        val transactionDao = database.transactionDao()
        val remoteConfigDao = database.remoteDatabaseConfigDao()

        val activeFilters = filterDao.getActiveFiltersList()

        // Check if sender matches any active filter rule
        val isMatched = if (activeFilters.isEmpty()) {
            // If no filters configured, default to parsing incoming messages
            true
        } else {
            val cleanSender = sender.trim().replace(" ", "").lowercase()
            activeFilters.any { filter ->
                val cleanFilter = filter.phoneNumberOrSenderId.trim().replace(" ", "").lowercase()
                cleanFilter == cleanSender ||
                        cleanSender.contains(cleanFilter) ||
                        cleanFilter.contains(cleanSender)
            }
        }

        if (!isMatched) {
            Log.d(TAG, "Ignored SMS from $sender: Does not match active target phone numbers or senders")
            return
        }

        Log.d(TAG, "Matched sender filter! Invoking AI extraction for SMS from $sender...")
        val parsed = GeminiSmsParser.parseSmsMessage(sender, body, timestampMillis)

        val transactionEntity = TransactionEntity(
            senderNumber = sender,
            merchantOrName = parsed.name,
            amount = parsed.amount,
            currency = parsed.currency,
            transactionNumber = parsed.transactionNumber,
            dateTimeMillis = parsed.dateTimeMillis,
            dateTimeString = parsed.dateTimeString,
            transactionType = parsed.transactionType,
            rawMessageBody = body,
            aiSummary = parsed.summary,
            isAutoTriggered = true,
            remoteSyncStatus = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val transactionForSave = transactionEntity.copy(
            gameWebhookStatus = if (GameWebhookClient.isEligiblePayment(transactionEntity)) "PENDING" else "NOT_APPLICABLE"
        )

        val id = transactionDao.insertTransaction(transactionForSave)
        var savedEntity = transactionForSave.copy(id = id)

        Log.d(TAG, "Saved AI extracted transaction #$id to local database: ${savedEntity.merchantOrName} - ${savedEntity.amount}")

        // Automatically push to user-provided Remote Database (Firebase / Neon / Supabase / REST API)
        try {
            val remoteConfig = remoteConfigDao.getConfigDirect()
            if (remoteConfig != null &&
                remoteConfig.isAutoSyncEnabled &&
                remoteConfig.databaseUrl.isNotBlank() &&
                !remoteConfig.databaseUrl.startsWith("https://your-project-id")
            ) {
                Log.d(TAG, "Triggering automatic push to remote database: ${remoteConfig.databaseType} (${remoteConfig.databaseUrl})")
                val syncResult = RemoteDatabaseSyncManager.syncTransaction(savedEntity, remoteConfig)
                if (syncResult.isSuccess) {
                    transactionDao.updateSyncStatus(id, "SYNCED", null, System.currentTimeMillis())
                    remoteConfigDao.updateSyncStatus("SYNCED", System.currentTimeMillis(), null)
                    savedEntity = savedEntity.copy(remoteSyncStatus = "SYNCED")
                    Log.d(TAG, "Successfully synced transaction #$id to remote database!")
                } else {
                    val error = syncResult.exceptionOrNull()?.message
                    transactionDao.updateSyncStatus(id, "FAILED", error, System.currentTimeMillis())
                    remoteConfigDao.updateSyncStatus("ERROR", System.currentTimeMillis(), error)
                    savedEntity = savedEntity.copy(remoteSyncStatus = "FAILED", remoteSyncError = error)
                    Log.w(TAG, "Remote database sync failed for transaction #$id: $error")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Remote sync error in receiver: ${e.message}", e)
        }

        val gameConfig = remoteConfigDao.getConfigDirect()
        if (savedEntity.gameWebhookStatus == "PENDING"
            && savedEntity.remoteSyncStatus == "SYNCED"
            && gameConfig?.isGameWebhookEnabled == true
        ) {
            val delivery = GameWebhookClient.sendVerifiedPayment(savedEntity, gameConfig)
            val status = if (delivery.isSuccess) "DELIVERED" else "FAILED"
            val error = delivery.exceptionOrNull()?.message
            val deliveredAt = System.currentTimeMillis()
            transactionDao.updateGameWebhookStatus(id, status, error, deliveredAt)
            savedEntity = savedEntity.copy(
                gameWebhookStatus = status,
                gameWebhookError = error,
                gameWebhookTime = deliveredAt
            )
            if (delivery.isFailure) {
                Log.w(TAG, "Game webhook delivery failed for transaction #$id: $error")
            }
        }

        NotificationHelper.showTransactionNotification(context, savedEntity)
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
