package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiSmsParser
import com.example.data.model.RemoteDatabaseConfigEntity
import com.example.data.model.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI SMS Ledger", appName)
    }

    @Test
    fun `test local sms parser extraction`() {
        val message = "Chase Alert: You made a withdrawal of $1,240.00 at ATM #4029. Ref #TXN-88219 on Aug 19, 14:32 PM."
        val result = GeminiSmsParser.extractWithLocalRegex("+1 (555) 012-3456", message, System.currentTimeMillis())

        assertEquals(1240.0, result.amount, 0.01)
        assertEquals("USD", result.currency)
        assertTrue(result.transactionNumber.contains("TXN-88219") || result.transactionNumber.contains("88219"))
        assertEquals("Withdrawal", result.transactionType)
    }

    @Test
    fun `test remote database config model`() {
        val config = RemoteDatabaseConfigEntity(
            databaseType = "NEON_POSTGRES",
            databaseUrl = "https://ep-test.neon.tech/api/transactions",
            apiKeyOrToken = "secret_token_123",
            tableNameOrPath = "transactions",
            isAutoSyncEnabled = true
        )

        assertEquals("NEON_POSTGRES", config.databaseType)
        assertEquals("https://ep-test.neon.tech/api/transactions", config.databaseUrl)
        assertTrue(config.isAutoSyncEnabled)
    }
}
