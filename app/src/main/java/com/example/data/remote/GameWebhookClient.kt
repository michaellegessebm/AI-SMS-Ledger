package com.example.data.remote

import com.example.data.model.RemoteDatabaseConfigEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class LedgerVerificationFields(
    val status: String,
    val verifiedAt: String?,
    val verified: Boolean
)

object GameWebhookClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun isEligiblePayment(transaction: TransactionEntity): Boolean {
        val type = transaction.transactionType.trim().lowercase(Locale.ROOT)
        return transaction.currency.trim().equals("ETB", ignoreCase = true)
            && (type.startsWith("credit") || type == "income")
            && transaction.transactionNumber.isNotBlank()
            && transaction.amount.isFinite()
            && transaction.amount % 1.0 == 0.0
            && transaction.amount > 0.0
    }

    fun verificationFields(transaction: TransactionEntity): LedgerVerificationFields {
        if (!isEligiblePayment(transaction)) {
            return LedgerVerificationFields(status = "PENDING", verifiedAt = null, verified = false)
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return LedgerVerificationFields(
            status = "PAID",
            verifiedAt = dateFormat.format(Date(transaction.createdAt)),
            verified = true
        )
    }

    suspend fun sendVerifiedPayment(
        transaction: TransactionEntity,
        config: RemoteDatabaseConfigEntity
    ): Result<String> {
        if (!config.isGameWebhookEnabled || config.gameWebhookUrl.isBlank() || config.gameWebhookToken.isBlank()) {
            return Result.failure(IllegalStateException("Game webhook is not configured"))
        }
        if (!isEligiblePayment(transaction)) {
            return Result.failure(IllegalArgumentException("Transaction is not a verified ETB credit"))
        }
        val verification = verificationFields(transaction)
        val payload = JSONObject().apply {
            put("source", "AI_SMS_LEDGER")
            put("event", "payment.received")
            put("transaction_number", transaction.transactionNumber.trim())
            put("amount", transaction.amount)
            put("currency", transaction.currency.trim().uppercase(Locale.ROOT))
            put("transaction_type", transaction.transactionType.trim())
            put("status", verification.status)
            put("verified", verification.verified)
            put("verified_at", verification.verifiedAt)
            put("merchant_name", transaction.merchantOrName.trim())
            put("sender_number", transaction.senderNumber.trim())
        }
        return postJson(config.gameWebhookUrl, config.gameWebhookToken, payload, isTest = false)
    }

    suspend fun testConnection(url: String, token: String): Result<String> {
        val payload = JSONObject().apply {
            put("source", "AI_SMS_LEDGER")
            put("transaction_number", "SYSTEM_TEST")
            put("amount", 0)
            put("currency", "ETB")
            put("transaction_type", "SYSTEM_TEST")
            put("status", "PENDING")
            put("verified", false)
            put("verified_at", JSONObject.NULL)
        }
        return postJson(url, token, payload, isTest = true)
    }

    private suspend fun postJson(
        url: String,
        token: String,
        payload: JSONObject,
        isTest: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val safeUrl = url.trim()
        val safeToken = token.trim()
        if (safeUrl.isBlank() || safeToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Enter the webhook URL and shared key"))
        }
        if (!safeUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Use an HTTPS webhook URL"))
        }

        try {
            val request = Request.Builder()
                .url(safeUrl)
                .header("Authorization", "Bearer $safeToken")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Game webhook returned HTTP ${response.code}")
                    )
                }
                val responseJson = runCatching { JSONObject(responseText) }.getOrNull()
                if (responseJson?.optString("status") != "success") {
                    return@withContext Result.failure(
                        IllegalStateException("Game webhook returned an unexpected response")
                    )
                }
                if (isTest && !responseJson.optBoolean("ignored", false)) {
                    return@withContext Result.failure(
                        IllegalStateException("Game did not confirm that the test event was ignored")
                    )
                }
                if (!isTest && responseJson?.optBoolean("ignored", false) == true) {
                    return@withContext Result.failure(
                        IllegalStateException("Game rejected this event; check its transaction fields")
                    )
                }
                Result.success(
                    if (isTest) "Connection works; the test event was safely ignored by the game."
                    else "Verified payment sent to the game."
                )
            }
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Could not reach the game webhook"))
        }
    }
}
