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
import java.util.Locale
import java.util.concurrent.TimeUnit

object GameWebhookClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val referencePattern = Regex(
        """(?i)\b(?:ref(?:erence)?|transaction(?:\s*(?:number|no|id|code))?|txn(?:\s*(?:number|no|id))?)\s*(?:is\s*)?[:#=.\-–—]?\s*([A-Z0-9][A-Z0-9._/-]{3,99})"""
    )

    fun isEligiblePayment(transaction: TransactionEntity): Boolean {
        val body = transaction.rawMessageBody.trim()
        val reference = normalizeReference(transaction.transactionNumber)
        val explicitReference = extractReference(body)
        val type = transaction.transactionType.trim().lowercase(Locale.ROOT)
        val evidence = listOf(transaction.senderNumber, transaction.merchantOrName, body)
            .joinToString(" ")
            .lowercase(Locale.ROOT)
        val testLike = reference.startsWith("TEST", ignoreCase = true)
            || evidence.contains("test payment webhook")
            || evidence.contains("telebirr test")
        val amount = transaction.amount
        val receiptAmount = extractIncomingAmount(body)

        return !testLike
            && transaction.currency.trim().equals("ETB", ignoreCase = true)
            && (type.startsWith("credit") || type == "income")
            && reference.length in 4..100
            && !reference.startsWith("TXN-", ignoreCase = true)
            && explicitReference != null
            && explicitReference.equals(reference, ignoreCase = true)
            && evidence.contains("telebirr")
            && amount.isFinite()
            && amount >= 1.0
            && amount % 1.0 == 0.0
            && receiptAmount != null
            && kotlin.math.abs(receiptAmount - amount) < 0.000001
    }

    suspend fun sendVerifiedPayment(
        transaction: TransactionEntity,
        config: RemoteDatabaseConfigEntity
    ): Result<String> {
        if (!config.isGameWebhookEnabled || config.gameWebhookUrl.isBlank() || config.gameWebhookToken.isBlank()) {
            return Result.failure(IllegalStateException("Game webhook is not configured"))
        }
        if (!isEligiblePayment(transaction)) {
            return Result.failure(IllegalArgumentException("SMS is not a verified matching Telebirr credit"))
        }

        val reference = normalizeReference(transaction.transactionNumber)
        val data = JSONObject().apply {
            put("transaction_id", reference)
            put("transaction_number", reference)
            put("amount", transaction.amount.toLong())
            put("currency", "ETB")
            put("transaction_type", "CREDIT")
            put("payer", transaction.merchantOrName.trim())
            put("sender_number", transaction.senderNumber.trim())
            put("account_or_provider", "Telebirr")
            put("status", "PAID")
            put("verified", true)
        }
        val payload = JSONObject().apply {
            put("event", "payment.confirmed")
            put("data", data)
            put("metadata", JSONObject().apply {
                put("source", "AI SMS Ledger")
                put("version", "1.0")
            })
        }
        return postJson(config.gameWebhookUrl, config.gameWebhookToken, payload, isTest = false)
    }

    suspend fun testConnection(url: String, token: String): Result<String> {
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter the shared webhook key before testing"))
        }
        val testUrl = resolveNoCreditTestUrl(url)
            ?: return Result.failure(IllegalArgumentException("Use the Live Keno payment webhook URL"))
        val payload = JSONObject().apply {
            put("event", "payment.confirmed")
            put("data", JSONObject().apply {
                put("transaction_number", "TEST-REF-CONNECTION")
                put("amount", 1)
                put("currency", "ETB")
                put("transaction_type", "CREDIT")
                put("status", "PAID")
                put("verified", true)
            })
        }
        return postJson(testUrl, null, payload, isTest = true)
    }

    private fun resolveNoCreditTestUrl(url: String): String? {
        val base = url.trim().trimEnd('/')
        return when {
            base.endsWith("/api/webhooks/sms-payment/test", ignoreCase = true) -> base
            base.endsWith("/api/webhooks/sms-payment", ignoreCase = true) -> "$base/test"
            base.endsWith("/api/payment-confirmation/test", ignoreCase = true) -> base
            base.endsWith("/api/payment-confirmation", ignoreCase = true) -> "$base/test"
            else -> null
        }
    }

    private suspend fun postJson(
        url: String,
        apiKey: String?,
        payload: JSONObject,
        isTest: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val safeUrl = url.trim()
        if (!safeUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Use an HTTPS webhook URL"))
        }
        if (!isTest && apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Enter the shared webhook key"))
        }
        try {
            val builder = Request.Builder()
                .url(safeUrl)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
            if (!isTest) builder.header("X-API-Key", apiKey!!)

            httpClient.newCall(builder.build()).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Game webhook returned HTTP " + response.code)
                    )
                }
                val responseJson = runCatching { JSONObject(responseText) }.getOrNull()
                    ?: return@withContext Result.failure(IllegalStateException("Game webhook returned invalid JSON"))
                if (responseJson.optString("status") != "success") {
                    return@withContext Result.failure(IllegalStateException("Game webhook did not confirm the request"))
                }
                if (isTest) {
                    if (!responseJson.optBoolean("testMode", false) || responseJson.optBoolean("credited", true)) {
                        return@withContext Result.failure(IllegalStateException("Game did not confirm a safe no-credit test"))
                    }
                    return@withContext Result.success("Connection works; this test did not change any balance.")
                }
                if (responseJson.optBoolean("ignored", false)) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Game ignored the event: " + responseJson.optString("reason", "check the SMS fields")
                        )
                    )
                }
                when {
                    responseJson.optBoolean("credited", false) ->
                        Result.success("Verified payment credited to the matching wallet.")
                    responseJson.optBoolean("duplicate", false) ->
                        Result.success("Payment was already processed; no duplicate credit was added.")
                    responseJson.optBoolean("reviewQueued", false) ->
                        Result.success("Payment received for admin review; the balance is not credited yet.")
                    else -> Result.failure(IllegalStateException("Game received the event but did not credit it."))
                }
            }
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Could not reach the game webhook"))
        }
    }

    private fun normalizeReference(reference: String): String =
        reference.trim().trimEnd('.', ',', ';', ':')

    private fun extractReference(body: String): String? {
        val match = referencePattern.find(body) ?: return null
        val value = normalizeReference(match.groupValues.getOrNull(1).orEmpty())
        return value.takeIf { it.length in 4..100 }
    }

    private fun extractIncomingAmount(body: String): Double? {
        val incoming = Regex("""(?i)\b(?:received|credited)\b""").find(body) ?: return null
        val textAfterIncoming = body.substring(incoming.range.last + 1)
        val patterns = listOf(
            Regex("""(?i)\bETB\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""),
            Regex("""(?i)\b([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:ETB|Birr)\b""")
        )
        for (pattern in patterns) {
            val value = pattern.find(textAfterIncoming)?.groupValues?.getOrNull(1) ?: continue
            return value.replace(",", "").toDoubleOrNull()
        }
        return null
    }
}
