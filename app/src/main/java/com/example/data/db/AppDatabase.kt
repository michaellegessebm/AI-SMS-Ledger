package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.RemoteDatabaseConfigDao
import com.example.data.dao.SenderFilterDao
import com.example.data.dao.TransactionDao
import com.example.data.model.RemoteDatabaseConfigEntity
import com.example.data.model.SenderFilterEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        SenderFilterEntity::class,
        RemoteDatabaseConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun senderFilterDao(): SenderFilterDao
    abstract fun remoteDatabaseConfigDao(): RemoteDatabaseConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_ledger_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(
                            database.senderFilterDao(),
                            database.transactionDao(),
                            database.remoteDatabaseConfigDao()
                        )
                    }
                }
            }
        }

        private suspend fun populateInitialData(
            filterDao: SenderFilterDao,
            transactionDao: TransactionDao,
            configDao: RemoteDatabaseConfigDao
        ) {
            // Default Remote Database configuration template
            val defaultConfig = RemoteDatabaseConfigEntity(
                id = 1,
                databaseType = "NEON_POSTGRES",
                databaseUrl = "postgresql://neondb_owner:npg_Y0KnDNIxHQX8@ep-raspy-glitter-axdefbiq-pooler.c-4.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require",
                apiKeyOrToken = "npg_Y0KnDNIxHQX8",
                tableNameOrPath = "transactions",
                isAutoSyncEnabled = true,
                lastSyncStatus = "NOT_CONFIGURED"
            )
            configDao.insertOrUpdateConfig(defaultConfig)

            // Initial default monitored sender rules
            val defaultFilters = listOf(
                SenderFilterEntity(
                    phoneNumberOrSenderId = "+1 (555) 012-3456",
                    label = "Bank Alerts",
                    isEnabled = true,
                    autoAiParse = true
                ),
                SenderFilterEntity(
                    phoneNumberOrSenderId = "Telebirr",
                    label = "Telebirr Mobile Money",
                    isEnabled = true,
                    autoAiParse = true
                ),
                SenderFilterEntity(
                    phoneNumberOrSenderId = "CBEBirr",
                    label = "Commercial Bank of Ethiopia",
                    isEnabled = true,
                    autoAiParse = true
                ),
                SenderFilterEntity(
                    phoneNumberOrSenderId = "MPESA",
                    label = "M-Pesa Safaricom",
                    isEnabled = true,
                    autoAiParse = true
                )
            )
            filterDao.insertFilters(defaultFilters)
        }
    }
}
