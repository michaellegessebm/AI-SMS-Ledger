package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ParsedSmsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object GeminiSmsParser {
    private const val TAG = "GeminiSmsParser"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseSmsMessage(sender: String, messageBody: String, timestampMillis: Long = System.currentTimeMillis()): ParsedSmsResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiResult = callGeminiApi(sender, messageBody, apiKey, timestampMillis)
                if (aiResult != null) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed, falling back to local extractor: ${e.message}")
            }
        }

        // Fallback local pattern extractor for offline/unconfigured API key
        return@withContext extractWithLocalRegex(sender, messageBody, timestampMillis)
    }

    private fun callGeminiApi(sender: String, messageBody: String, apiKey: String, timestampMillis: Long): ParsedSmsResult? {
        val prompt = """
            You are an intelligent financial SMS parser. Extract structured financial transaction details from this SMS text.
            Pay special attention to mobile money systems (such as Telebirr, CBEBirr, M-Pesa) and bank SMS alerts.

            SMS Sender: "$sender"
            SMS Body: "$messageBody"

            Strict Extraction Rules:
            1. "transaction_number": Extract the exact transaction code or reference ID.
               - For example, if the SMS says "Your transaction number is DHK115G4WD.", the transaction_number MUST be "DHK115G4WD".
               - If it says "Ref: ...", "Txn ID: ...", "Transaction ID is ...", extract that reference code.
               - Do not include trailing punctuation (such as periods or commas).
               - Never use phone numbers or balances as transaction numbers.
            2. "amount": Extract the actual transacted / received / paid / debited amount (e.g. 1.00 in "You have received ETB 1.00").
               - CRITICAL: Do NOT extract the current/remaining balance (e.g. "Your current E-Money Account balance is ETB 92.08") as the transaction amount.
            3. "currency": The 3-letter currency code (e.g., "ETB", "USD", "EUR", "GBP", "KES", "INR").
            4. "name": The merchant, counterparty person, bank, or service (e.g. "Legesse Gebrehanna", "Telebirr", "Amazon", "Chase").
            5. "date_time": The transaction date and time mentioned in the text (e.g. "20/08/2026 22:24:21").
            6. "transaction_type": One of: "Credit" (if received/deposited/credited), "Debit" (if spent/debited/withdrawn), "Transfer", "Payment", "Withdrawal", "POS Charge".
            7. "summary": A concise 1-sentence transaction summary (e.g. "Received ETB 1.00 from Legesse Gebrehanna via Telebirr").

            Return ONLY valid JSON matching this exact schema:
            {
              "name": "Legesse Gebrehanna",
              "amount": 1.00,
              "currency": "ETB",
              "transaction_number": "DHK115G4WD",
              "date_time": "20/08/2026 22:24:21",
              "transaction_type": "Credit",
              "summary": "Received ETB 1.00 from Legesse Gebrehanna"
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", prompt))
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.1)
                val responseFormat = JSONObject().apply {
                    put("mimeType", "application/json")
                }
                put("responseFormat", responseFormat)
            }
            put("generationConfig", generationConfig)
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val urlWithKey = "$API_URL?key=$apiKey"

        val request = Request.Builder()
            .url(urlWithKey)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API error code: ${response.code}, body: ${response.body?.string()}")
            return null
        }

        val responseString = response.body?.string() ?: return null
        val rootJson = JSONObject(responseString)
        val candidates = rootJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val text = parts.getJSONObject(0).optString("text")
        if (text.isBlank()) return null

        // Clean json text if wrapped in markdown codeblocks
        val cleanedJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsedJson = JSONObject(cleanedJson)
        val name = parsedJson.optString("name", "").ifBlank {
            extractNameOrMerchant(sender, messageBody)
        }
        val amount = parsedJson.optDouble("amount", 0.0)
        val currency = parsedJson.optString("currency", "").ifBlank {
            extractCurrency(messageBody)
        }
        val txnNum = parsedJson.optString("transaction_number", "").trim().trimEnd('.', ',', ';').ifBlank {
            extractTransactionId(messageBody)
        }
        val dateTime = parsedJson.optString("date_time", "").ifBlank {
            extractDateTime(messageBody, timestampMillis)
        }
        val type = parsedJson.optString("transaction_type", "Credit").ifBlank {
            extractTransactionType(messageBody)
        }
        val summary = parsedJson.optString("summary", "").ifBlank {
            "$type of $currency $amount ${if (name.isNotBlank()) "at/from $name" else ""} (Ref: $txnNum)"
        }

        return ParsedSmsResult(
            name = name,
            amount = if (amount > 0) amount else extractAmount(messageBody),
            currency = currency,
            transactionNumber = txnNum,
            dateTimeString = dateTime,
            dateTimeMillis = timestampMillis,
            transactionType = type,
            summary = summary,
            confidence = 0.98f
        )
    }

    /**
     * Highly resilient regex & rule-based parser for financial SMS messages.
     * Supports multi-currency (USD, EUR, GBP, ETB, KES, INR, etc.) and major banking/fintech formats.
     */
    fun extractWithLocalRegex(sender: String, message: String, timestampMillis: Long): ParsedSmsResult {
        val amount = extractAmount(message)
        val currency = extractCurrency(message)
        val txnId = extractTransactionId(message)
        val txnType = extractTransactionType(message)
        val name = extractNameOrMerchant(sender, message)
        val timeString = extractDateTime(message, timestampMillis)

        val summary = when {
            amount > 0 -> "$txnType of $currency $amount from/at $name (Ref: $txnId)"
            else -> "Message from $name (Ref: $txnId)"
        }

        return ParsedSmsResult(
            name = name,
            amount = amount,
            currency = currency,
            transactionNumber = txnId,
            dateTimeString = timeString,
            dateTimeMillis = timestampMillis,
            transactionType = txnType,
            summary = summary,
            confidence = 0.90f
        )
    }

    private fun extractAmount(message: String): Double {
        // Priority 1: Action verb + optional currency + amount
        val actionVerbPatterns = listOf(
            Pattern.compile("""(?i)(?:received|credited|sent|paid|debited|charged|transferred|withdrew|spent|recharged)\s+(?:(?:ETB|USD|EUR|GBP|KES|INR|\$|€|£|₹|Birr|Ksh)\s*)?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)"""),
            Pattern.compile("""(?i)(?:(?:ETB|USD|EUR|GBP|KES|INR|\$|€|£|₹|Birr|Ksh)\s*)([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s+(?:from|to|at|by|via|for|credited|debited|paid|received)"""),
            Pattern.compile("""(?i)(?:amount|amt|total)[\s:\-–—]*(?:(?:ETB|USD|EUR|GBP|KES|INR|\$|€|£|₹|Birr|Ksh)\s*)?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""")
        )

        for (pattern in actionVerbPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val rawNumber = matcher.group(1)?.replace(",", "") ?: continue
                val parsed = rawNumber.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }

        // Priority 2: Currency symbol or code followed by numbers (skipping balance statements)
        val currencyPattern = Pattern.compile("""(?i)(?:[\$€£₹]|USD|EUR|GBP|ETB|KES|INR|Birr)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""")
        val matcher = currencyPattern.matcher(message)
        while (matcher.find()) {
            val matchStart = matcher.start()
            val prefix = message.substring((matchStart - 25).coerceAtLeast(0), matchStart).lowercase()
            // Ignore if this is the account balance clause
            if (prefix.contains("balance") || prefix.contains("bal:") || prefix.contains("bal is") || prefix.contains("acc bal")) {
                continue
            }
            val rawNumber = matcher.group(1)?.replace(",", "") ?: continue
            val parsed = rawNumber.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                return parsed
            }
        }

        // Priority 3: Fallback any decimal number not in a balance clause
        val decimalPattern = Pattern.compile("""([0-9]+(?:\.[0-9]{2}))""")
        val decMatcher = decimalPattern.matcher(message)
        while (decMatcher.find()) {
            val matchStart = decMatcher.start()
            val prefix = message.substring((matchStart - 25).coerceAtLeast(0), matchStart).lowercase()
            if (prefix.contains("balance") || prefix.contains("bal")) continue
            val parsed = decMatcher.group(1)?.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                return parsed
            }
        }

        return 0.0
    }

    private fun extractCurrency(message: String): String {
        val upper = message.uppercase()
        return when {
            upper.contains("ETB") || upper.contains("BIRR") -> "ETB"
            upper.contains("KES") || upper.contains("KSH") -> "KES"
            message.contains("$") || upper.contains("USD") -> "USD"
            message.contains("€") || upper.contains("EUR") -> "EUR"
            message.contains("£") || upper.contains("GBP") -> "GBP"
            message.contains("₹") || upper.contains("INR") -> "INR"
            else -> "ETB"
        }
    }

    fun extractTransactionId(message: String): String {
        // High priority patterns for "Your transaction number is DHK115G4WD.", "transaction number is DHK115G4WD", "txn id:", etc.
        val priorityPatterns = listOf(
            // Handles: "Your transaction number is DHK115G4WD.", "transaction number is DHK115G4WD", "transaction number : DHK115G4WD"
            Pattern.compile("""(?i)(?:your\s+)?(?:transaction|txn|trans|reference|ref)\s+(?:number|no|id|code|num)\s*(?:is|:|=|-|–)?\s*([A-Za-z0-9\-_]{4,30})"""),
            // Handles: "transaction id is XXX", "reference code is XXX"
            Pattern.compile("""(?i)(?:transaction|reference|txn)\s+(?:id|code)\s*(?:is|:|=|-|–)?\s*([A-Za-z0-9\-_]{4,30})"""),
            // Handles: "Ref: ...", "Txn ID: ...", "Ref #..."
            Pattern.compile("""(?i)(?:ref(?:erence)?|txn|trans\s*id)[\s#:\-–—]+([A-Za-z0-9\-_]{4,25})"""),
            // Handles: "#TXN-88219"
            Pattern.compile("""#([A-Za-z0-9\-]{5,20})"""),
            // Handles: Telebirr / CBE / M-Pesa standard alphanumeric 10-character transaction codes (e.g. DHK115G4WD)
            Pattern.compile("""\b([A-Z0-9]{10})\b"""),
            Pattern.compile("""\b(TXN-[0-9]{4,10})\b""")
        )

        for (pattern in priorityPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()?.trimEnd('.', ',', ';', ':', '!', '?')
                if (!candidate.isNullOrBlank() && !candidate.equals("is", ignoreCase = true) && !candidate.equals("on", ignoreCase = true)) {
                    return candidate
                }
            }
        }

        // Fallback alphanumeric token
        val genericToken = Pattern.compile("""\b[A-Z0-9]{8,14}\b""").matcher(message)
        while (genericToken.find()) {
            val token = genericToken.group()
            if (token.any { it.isDigit() } && token.any { it.isLetter() }) {
                return token
            }
        }

        return "TXN-${(10000..99999).random()}"
    }

    private fun extractTransactionType(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("received") || lower.contains("credited") || lower.contains("deposit") || lower.contains("income") -> "Credit"
            lower.contains("sent") || lower.contains("transferred") || lower.contains("transfer") -> "Transfer"
            lower.contains("paid") || lower.contains("payment") || lower.contains("bill") -> "Payment"
            lower.contains("withdrawal") || lower.contains("withdrew") || lower.contains("atm") -> "Withdrawal"
            lower.contains("pos") || lower.contains("card purchase") || lower.contains("charged") -> "POS Charge"
            lower.contains("debit") || lower.contains("debited") -> "Debit"
            else -> "Transaction Detected"
        }
    }

    private fun extractNameOrMerchant(sender: String, message: String): String {
        // Pattern 1: "from Legesse Gebrehanna(2519****7054)" or "from Legesse Gebrehanna on..."
        val fromPersonPattern = Pattern.compile("""(?i)from\s+([A-Za-z\s'&.\-]{2,40}?)(?:\s*\([0-9\*\+]+\)|\s+on\s+|\.|\,|\s+for|\s+your|$)""")
        val fromMatcher = fromPersonPattern.matcher(message)
        if (fromMatcher.find()) {
            val name = fromMatcher.group(1)?.trim()?.trimEnd('.', ',')
            if (!name.isNullOrBlank() && name.length > 2 && !name.equals("you", ignoreCase = true) && !name.equals("account", ignoreCase = true)) {
                return name
            }
        }

        // Pattern 2: "at <Merchant>", "to <Recipient>", "paid to <Merchant>"
        val merchantPattern = Pattern.compile("""(?i)(?:at|to|for|merchant|vendor|by)[\s:]+([A-Za-z0-9\s'&.]{3,35}?)(?:\.|\,|\s*\(|\s+on|\s+ref|\s+time|\s+amt|\s+ending|\#|$)""")
        val matcher = merchantPattern.matcher(message)
        if (matcher.find()) {
            val name = matcher.group(1)?.trim()?.trimEnd('.', ',')
            if (!name.isNullOrBlank() && name.length > 2 && !name.equals("you", ignoreCase = true) && !name.equals("your", ignoreCase = true)) {
                return name
            }
        }

        // Pattern 3: Look for Telebirr / CBE / M-Pesa / Bank in message or sender
        if (message.contains("telebirr", ignoreCase = true) || sender.contains("telebirr", ignoreCase = true)) {
            return "Telebirr"
        }

        // Return sender label or clean sender name
        if (sender.isNotBlank() && !sender.startsWith("+") && sender.all { it.isLetterOrDigit() || it.isWhitespace() }) {
            return sender.trim()
        }
        return if (sender.isNotBlank()) sender.trim() else "Unknown Counterparty"
    }

    private fun extractDateTime(message: String, timestampMillis: Long): String {
        val datePattern = Pattern.compile("""(?i)(?:on\s+)?([0-9]{1,2}[/\-\.][0-9]{1,2}[/\-\.][0-9]{2,4}(?:\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?(?:\s*[AaPp][Mm])?)?)""")
        val matcher = datePattern.matcher(message)
        if (matcher.find()) {
            val dt = matcher.group(1)?.trim()
            if (!dt.isNullOrBlank()) {
                return dt
            }
        }
        return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))
    }
}
