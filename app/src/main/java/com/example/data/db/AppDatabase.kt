package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun senderFilterDao(): SenderFilterDao
    abstract fun remoteDatabaseConfigDao(): RemoteDatabaseConfigDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE remote_database_config ADD COLUMN gameWebhookUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE remote_database_config ADD COLUMN gameWebhookToken TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE remote_database_config ADD COLUMN isGameWebhookEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN gameWebhookStatus TEXT NOT NULL DEFAULT 'NOT_APPLICABLE'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN gameWebhookError TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN gameWebhookTime INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_ledger_database"
                )
                .addMigrations(MIGRATION_2_3)
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
                databaseUrl = "",
                apiKeyOrToken = "",
                tableNameOrPath = "transactions",
                isAutoSyncEnabled = false,
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
