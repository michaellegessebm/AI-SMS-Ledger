package com.example.data.repository

import com.example.data.dao.RemoteDatabaseConfigDao
import com.example.data.dao.SenderFilterDao
import com.example.data.dao.TransactionDao
import com.example.data.model.RemoteDatabaseConfigEntity
import com.example.data.model.SenderFilterEntity
import com.example.data.model.TransactionEntity
import com.example.data.remote.GameWebhookClient
import com.example.data.remote.RemoteDatabaseSyncManager
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val senderFilterDao: SenderFilterDao,
    private val remoteDatabaseConfigDao: RemoteDatabaseConfigDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val allFilters: Flow<List<SenderFilterEntity>> = senderFilterDao.getAllFiltersFlow()
    val activeFilters: Flow<List<SenderFilterEntity>> = senderFilterDao.getActiveFiltersFlow()
    val totalCount: Flow<Int> = transactionDao.getTransactionCountFlow()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncomeFlow()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpenseFlow()
    val remoteConfig: Flow<RemoteDatabaseConfigEntity?> = remoteDatabaseConfigDao.getConfig()

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> {
        return if (query.isBlank()) {
            transactionDao.getAllTransactionsFlow()
        } else {
            transactionDao.searchTransactionsFlow(query.trim())
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> {
        return transactionDao.insertTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
    }

    suspend fun insertFilter(filter: SenderFilterEntity): Long {
        return senderFilterDao.insertFilter(filter)
    }

    suspend fun updateFilter(filter: SenderFilterEntity) {
        senderFilterDao.updateFilter(filter)
    }

    suspend fun deleteFilter(filter: SenderFilterEntity) {
        senderFilterDao.deleteFilter(filter)
    }

    suspend fun deleteFilterById(id: Long) {
        senderFilterDao.deleteFilterById(id)
    }

    suspend fun getActiveFiltersList(): List<SenderFilterEntity> {
        return senderFilterDao.getActiveFiltersList()
    }

    suspend fun isSenderMonitored(sender: String): Boolean {
        val cleanSender = sender.trim().replace(" ", "").lowercase()
        val active = senderFilterDao.getActiveFiltersList()
        if (active.isEmpty()) return true
        return active.any { filter ->
            val cleanFilter = filter.phoneNumberOrSenderId.trim().replace(" ", "").lowercase()
            cleanFilter == cleanSender ||
                    cleanSender.contains(cleanFilter) ||
                    cleanFilter.contains(cleanSender)
        }
    }

    // Remote Database Methods
    suspend fun getRemoteConfigDirect(): RemoteDatabaseConfigEntity? {
        return remoteDatabaseConfigDao.getConfigDirect()
    }

    suspend fun saveRemoteConfig(config: RemoteDatabaseConfigEntity) {
        remoteDatabaseConfigDao.insertOrUpdateConfig(config)
    }

    suspend fun testRemoteConnection(config: RemoteDatabaseConfigEntity): Result<String> {
        val result = RemoteDatabaseSyncManager.testConnection(config)
        if (result.isSuccess) {
            remoteDatabaseConfigDao.updateSyncStatus("CONNECTED", System.currentTimeMillis(), null)
        } else {
            remoteDatabaseConfigDao.updateSyncStatus("ERROR", System.currentTimeMillis(), result.exceptionOrNull()?.message)
        }
        return result
    }

    suspend fun syncTransactionToRemote(transaction: TransactionEntity): Result<String> {
        val config = remoteDatabaseConfigDao.getConfigDirect()
        if (config == null || !config.isAutoSyncEnabled || config.databaseUrl.isBlank() || config.databaseUrl.startsWith("https://your-project-id")) {
            return Result.failure(Exception("Remote database not configured"))
        }

        val result = RemoteDatabaseSyncManager.syncTransaction(transaction, config)
        if (result.isSuccess) {
            transactionDao.updateSyncStatus(transaction.id, "SYNCED", null, System.currentTimeMillis())
            remoteDatabaseConfigDao.updateSyncStatus("SYNCED", System.currentTimeMillis(), null)
            if (config.isGameWebhookEnabled && GameWebhookClient.isEligiblePayment(transaction)) {
                deliverGameWebhook(transaction, config)
            }
        } else {
            val errorMsg = result.exceptionOrNull()?.message
            transactionDao.updateSyncStatus(transaction.id, "FAILED", errorMsg, System.currentTimeMillis())
            remoteDatabaseConfigDao.updateSyncStatus("ERROR", System.currentTimeMillis(), errorMsg)
        }
        return result
    }

    suspend fun syncAllPendingTransactions(): Result<Int> {
        val config = remoteDatabaseConfigDao.getConfigDirect()
            ?: return Result.failure(Exception("Remote database configuration not found"))

        if (config.databaseUrl.isBlank() || config.databaseUrl.startsWith("https://your-project-id")) {
            return Result.failure(Exception("Please enter a valid Database API URL in Cloud Settings"))
        }

        val pending = transactionDao.getPendingSyncTransactions()
        if (pending.isEmpty()) {
            return Result.success(0)
        }

        var successCount = 0
        for (txn in pending) {
            val syncResult = RemoteDatabaseSyncManager.syncTransaction(txn, config)
            if (syncResult.isSuccess) {
                transactionDao.updateSyncStatus(txn.id, "SYNCED", null, System.currentTimeMillis())
                if (config.isGameWebhookEnabled && GameWebhookClient.isEligiblePayment(txn)) {
                    deliverGameWebhook(txn, config)
                }
                successCount++
            } else {
                transactionDao.updateSyncStatus(txn.id, "FAILED", syncResult.exceptionOrNull()?.message, System.currentTimeMillis())
            }
        }

        remoteDatabaseConfigDao.updateSyncStatus(
            if (successCount > 0) "SYNCED" else "ERROR",
            System.currentTimeMillis(),
            if (successCount == pending.size) null else "Partially synced: $successCount of ${pending.size}"
        )

        return Result.success(successCount)
    }

    suspend fun retryPendingGameWebhooks(): Result<Int> {
        val config = remoteDatabaseConfigDao.getConfigDirect()
            ?: return Result.failure(Exception("Remote database configuration not found"))
        if (!config.isGameWebhookEnabled || config.gameWebhookUrl.isBlank() || config.gameWebhookToken.isBlank()) {
            return Result.failure(Exception("Save and enable the game webhook first"))
        }

        val pending = transactionDao.getPendingGameWebhookTransactions()
        var successCount = 0
        for (transaction in pending) {
            if (deliverGameWebhook(transaction, config)) successCount++
        }
        return Result.success(successCount)
    }

    private suspend fun deliverGameWebhook(
        transaction: TransactionEntity,
        config: RemoteDatabaseConfigEntity
    ): Boolean {
        val result = GameWebhookClient.sendVerifiedPayment(transaction, config)
        val status = if (result.isSuccess) "DELIVERED" else "FAILED"
        transactionDao.updateGameWebhookStatus(
            transaction.id,
            status,
            result.exceptionOrNull()?.message,
            System.currentTimeMillis()
        )
        return result.isSuccess
    }
}
