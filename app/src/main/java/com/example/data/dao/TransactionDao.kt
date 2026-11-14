package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE merchantOrName LIKE '%' || :query || '%' OR transactionNumber LIKE '%' || :query || '%' OR rawMessageBody LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchTransactionsFlow(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionType = :type ORDER BY createdAt DESC")
    fun getTransactionsByTypeFlow(type: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM transactions WHERE remoteSyncStatus != 'SYNCED' ORDER BY createdAt DESC")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET remoteSyncStatus = :status, remoteSyncError = :error, remoteSyncTime = :time WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, error: String?, time: Long)

    @Query("SELECT * FROM transactions WHERE gameWebhookStatus IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingGameWebhookTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET gameWebhookStatus = :status, gameWebhookError = :error, gameWebhookTime = :time WHERE id = :id")
    suspend fun updateGameWebhookStatus(id: Long, status: String, error: String?, time: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCountFlow(): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE UPPER(transactionType) IN ('CREDIT', 'RECEIVED', 'INCOME', 'DEPOSIT')
           OR UPPER(transactionType) LIKE '%CREDIT%'
           OR UPPER(transactionType) LIKE '%RECEIV%'
           OR UPPER(transactionType) LIKE '%INCOME%'
           OR UPPER(transactionType) LIKE '%DEPOSIT%'
    """)
    fun getTotalIncomeFlow(): Flow<Double?>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE UPPER(transactionType) IN ('DEBIT', 'SENT', 'PAYMENT', 'WITHDRAWAL', 'TRANSFER', 'CHARGE', 'POS CHARGE', 'PURCHASE')
           OR UPPER(transactionType) LIKE '%DEBIT%'
           OR UPPER(transactionType) LIKE '%SENT%'
           OR UPPER(transactionType) LIKE '%PAY%'
           OR UPPER(transactionType) LIKE '%WITHDRAW%'
           OR UPPER(transactionType) LIKE '%TRANSFER%'
           OR UPPER(transactionType) LIKE '%POS%'
           OR UPPER(transactionType) LIKE '%CHARGE%'
           OR UPPER(transactionType) LIKE '%PURCHASE%'
    """)
    fun getTotalExpenseFlow(): Flow<Double?>
}
