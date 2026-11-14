package com.example.data.remote

import android.util.Log
import com.example.data.model.RemoteDatabaseConfigEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

data class ParsedPostgresConfig(
    val httpEndpoint: String,
    val host: String,
    val user: String,
    val password: String,
    val database: String,
    val fullConnectionString: String
)

object RemoteDatabaseSyncManager {
    private const val TAG = "RemoteDbSync"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Parses a PostgreSQL / Neon connection string:
     * postgresql://<your-user>:<your-password>@<your-host>/<your-database>
     */
    fun parsePostgresUri(rawUri: String): ParsedPostgresConfig? {
        val trimmed = rawUri.trim()
        if (!trimmed.startsWith("postgresql://", ignoreCase = true) && !trimmed.startsWith("postgres://", ignoreCase = true)) {
            return null
        }

        return try {
            val uri = URI(trimmed)
            val host = uri.host ?: ""
            val userInfo = uri.userInfo ?: ""
            val user = if (userInfo.contains(":")) userInfo.substringBefore(":") else userInfo
            val password = if (userInfo.contains(":")) userInfo.substringAfter(":") else ""
            val database = uri.path?.removePrefix("/")?.substringBefore("?") ?: "neondb"
            val httpEndpoint = "https://$host/sql"

            ParsedPostgresConfig(
                httpEndpoint = httpEndpoint,
                host = host,
                user = user,
                password = password,
                database = database,
                fullConnectionString = trimmed
            )
        } catch (e: Exception) {
            // Regex fallback in case URI parsing fails due to special characters in password
            try {
                val regex = """postgre(?:sql)?://([^:]+):([^@]+)@([^/]+)/([^?]+)(?:\?.*)?""".toRegex()
                val match = regex.find(trimmed)
                if (match != null) {
                    val (user, password, host, database) = match.destructured
                    ParsedPostgresConfig(
                        httpEndpoint = "https://$host/sql",
                        host = host,
                        user = user,
                        password = password,
                        database = database,
                        fullConnectionString = trimmed
                    )
                } else null
            } catch (ex: Exception) {
                null
            }
        }
    }

    /**
     * Sends the extracted transaction to the user's remote database endpoint
     */
    suspend fun syncTransaction(
        transaction: TransactionEntity,
        config: RemoteDatabaseConfigEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        if (config.databaseUrl.isBlank() || config.databaseUrl.startsWith("https://your-project-id")) {
            return@withContext Result.failure(Exception("Remote database URL is not configured yet."))
        }

        try {
            val postgresParsed = parsePostgresUri(config.databaseUrl)
            val isNeonPostgres = config.databaseType.equals("NEON_POSTGRES", ignoreCase = true) || postgresParsed != null

            val targetUrl = formatTargetUrl(config)
            val requestBuilder = Request.Builder().url(targetUrl)

            val requestBody = if (isNeonPostgres) {
                val tableName = config.tableNameOrPath.trim().ifBlank { "transactions" }
                val sqlInsert = """
                    INSERT INTO $tableName (
                        sender_number, merchant_name, amount, currency,
                        transaction_number, transaction_type, date_time_string,
                        date_time_millis, raw_message_body, ai_summary, is_auto_triggered
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11);
                """.trimIndent()

                val jsonPayload = JSONObject().apply {
                    put("query", sqlInsert)
                    put("params", JSONArray().apply {
                        put(transaction.senderNumber)
                        put(transaction.merchantOrName)
                        put(transaction.amount)
                        put(transaction.currency)
                        put(transaction.transactionNumber)
                        put(transaction.transactionType)
                        put(transaction.dateTimeString)
                        put(transaction.dateTimeMillis)
                        put(transaction.rawMessageBody)
                        put(transaction.aiSummary)
                        put(transaction.isAutoTriggered)
                    })
                }
                jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
            } else {
                val jsonPayload = buildTransactionJson(transaction, config)
                jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
            }

            requestBuilder.post(requestBody)
            applyAuthHeaders(requestBuilder, config, postgresParsed)

            val request = requestBuilder.build()
            Log.d(TAG, "Syncing transaction #${transaction.id} to remote DB: ${request.url}")

            val response = httpClient.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Log.d(TAG, "Successfully synced to remote DB ($responseCode): $responseBody")
                Result.success("Synced to remote DB ($responseCode)")
            } else {
                Log.e(TAG, "Remote DB sync failed ($responseCode): $responseBody")
                Result.failure(Exception("HTTP $responseCode: ${responseBody.take(200)}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error syncing to remote DB: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Test connection to user's database API / URL
     */
    suspend fun testConnection(
        config: RemoteDatabaseConfigEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        if (config.databaseUrl.isBlank()) {
            return@withContext Result.failure(Exception("Please enter a valid Database URL or Connection String."))
        }

        try {
            val postgresParsed = parsePostgresUri(config.databaseUrl)
            val isNeonPostgres = config.databaseType.equals("NEON_POSTGRES", ignoreCase = true) || postgresParsed != null

            val targetUrl = formatTargetUrl(config)
            val requestBuilder = Request.Builder().url(targetUrl)

            val requestBody = if (isNeonPostgres) {
                val tableName = config.tableNameOrPath.trim().ifBlank { "transactions" }
                // Create table if not exists
                val testSql = """
                    CREATE TABLE IF NOT EXISTS $tableName (
                        id SERIAL PRIMARY KEY,
                        sender_number VARCHAR(100),
                        merchant_name VARCHAR(255),
                        amount NUMERIC(12, 2),
                        currency VARCHAR(10),
                        transaction_number VARCHAR(150),
                        transaction_type VARCHAR(50),
                        date_time_string VARCHAR(100),
                        date_time_millis BIGINT,
                        raw_message_body TEXT,
                        ai_summary TEXT,
                        is_auto_triggered BOOLEAN DEFAULT true,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                    );
                """.trimIndent()
                val jsonPayload = JSONObject().apply {
                    put("query", testSql)
                }
                jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
            } else {
                val testTransaction = TransactionEntity(
                    id = 0,
                    senderNumber = "SYSTEM_TEST",
                    merchantOrName = "AI SMS Ledger Ping",
                    amount = 1.00,
                    currency = "USD",
                    transactionNumber = "TEST-${System.currentTimeMillis() % 10000}",
                    dateTimeString = "Just now",
                    transactionType = "SYSTEM_TEST",
                    rawMessageBody = "AI SMS Ledger connectivity test",
                    aiSummary = "Connection test",
                    isAutoTriggered = false
                )
                val jsonPayload = buildTransactionJson(testTransaction, config)
                jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
            }

            requestBuilder.post(requestBody)
            applyAuthHeaders(requestBuilder, config, postgresParsed)

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val extraInfo = if (isNeonPostgres) "Neon PostgreSQL connected & '$config.tableNameOrPath' table verified!" else "Server acknowledged payload"
                Result.success("Connected successfully! (HTTP $responseCode: $extraInfo)")
            } else {
                Result.failure(Exception("Remote Server returned HTTP $responseCode: ${responseBody.take(180)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun formatTargetUrl(config: RemoteDatabaseConfigEntity): String {
        val trimmed = config.databaseUrl.trim()
        val path = config.tableNameOrPath.trim().ifBlank { "transactions" }

        // If it is a Postgres connection string, convert to Neon Serverless HTTP API
        val postgresParsed = parsePostgresUri(trimmed)
        if (postgresParsed != null) {
            return postgresParsed.httpEndpoint
        }

        var url = trimmed

        // Ensure url has scheme
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }

        when (config.databaseType.uppercase()) {
            "FIREBASE" -> {
                if (!url.endsWith(".json")) {
                    url = if (url.endsWith("/")) {
                        "$url$path.json"
                    } else if (url.endsWith(".firebaseio.com")) {
                        "$url/$path.json"
                    } else {
                        "$url.json"
                    }
                }
                if (config.apiKeyOrToken.isNotBlank()) {
                    val separator = if (url.contains("?")) "&" else "?"
                    url = "$url${separator}auth=${config.apiKeyOrToken.trim()}"
                }
            }
            "SUPABASE" -> {
                if (!url.contains("/rest/v1/")) {
                    val cleanBase = url.removeSuffix("/")
                    url = "$cleanBase/rest/v1/$path"
                }
            }
            "NEON_POSTGRES" -> {
                // If it's just the host or base URL e.g. https://ep-xxx.neon.tech
                if (!url.endsWith("/sql")) {
                    val cleanBase = url.removeSuffix("/")
                    url = "$cleanBase/sql"
                }
            }
            "CUSTOM_REST" -> {
                // Use the provided URL as-is
            }
        }
        return url
    }

    private fun applyAuthHeaders(
        builder: Request.Builder,
        config: RemoteDatabaseConfigEntity,
        postgresParsed: ParsedPostgresConfig? = null
    ) {
        val isNeonPostgres = config.databaseType.equals("NEON_POSTGRES", ignoreCase = true) || postgresParsed != null
        if (isNeonPostgres) {
            val connStr = postgresParsed?.fullConnectionString
                ?: config.databaseUrl.trim().takeIf { it.startsWith("postgres", ignoreCase = true) }
                ?: if (config.apiKeyOrToken.isNotBlank()) {
                    try {
                        val host = URI(formatTargetUrl(config)).host ?: ""
                        "postgresql://<your-user>:<your-password>@<your-host>/<your-database>"
                    } catch (e: Exception) {
                        config.databaseUrl.trim()
                    }
                } else {
                    config.databaseUrl.trim()
                }

            builder.header("Neon-Connection-String", connStr)
            return
        }

        val token = config.apiKeyOrToken.trim()
        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        when (config.databaseType.uppercase()) {
            "SUPABASE" -> {
                if (token.isNotBlank()) {
                    builder.header("apikey", token)
                    builder.header("Prefer", "return=minimal")
                }
            }
        }
    }

    private fun buildTransactionJson(
        transaction: TransactionEntity,
        config: RemoteDatabaseConfigEntity
    ): JSONObject {
        return JSONObject().apply {
            put("id", transaction.id)
            put("sender_number", transaction.senderNumber)
            put("merchant_name", transaction.merchantOrName)
            put("amount", transaction.amount)
            put("currency", transaction.currency)
            put("transaction_number", transaction.transactionNumber)
            put("transaction_type", transaction.transactionType)
            put("date_time_string", transaction.dateTimeString)
            put("date_time_millis", transaction.dateTimeMillis)
            put("raw_message_body", transaction.rawMessageBody)
            put("ai_summary", transaction.aiSummary)
            put("is_auto_triggered", transaction.isAutoTriggered)
            put("source", "AI_SMS_LEDGER")
            put("synced_at", System.currentTimeMillis())
        }
    }
}
