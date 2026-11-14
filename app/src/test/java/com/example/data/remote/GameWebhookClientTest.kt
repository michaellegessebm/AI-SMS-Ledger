package com.example.data.remote

import com.example.data.model.TransactionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameWebhookClientTest {
    private fun payment(
        reference: String = "FT26049X9L2",
        body: String = "You have received ETB 250.00 from Abebe. Ref: FT26049X9L2. Balance is ETB 1,540.00",
        type: String = "Credit",
        currency: String = "ETB",
        amount: Double = 250.0,
        sender: String = "Telebirr"
    ) = TransactionEntity(
        senderNumber = sender,
        merchantOrName = "Abebe",
        amount = amount,
        currency = currency,
        transactionNumber = reference,
        transactionType = type,
        rawMessageBody = body,
        aiSummary = "Received ETB 250.00 from Abebe"
    )

    @Test
    fun acceptsMatchingIncomingTelebirrReceipt() {
        assertTrue(GameWebhookClient.isEligiblePayment(payment()))
    }

    @Test
    fun rejectsReferenceMismatch() {
        assertFalse(
            GameWebhookClient.isEligiblePayment(
                payment(
                    reference = "FT26049X9L2",
                    body = "You have received ETB 250.00 from Abebe. Ref: FT26049X9L3."
                )
            )
        )
    }

    @Test
    fun rejectsSyntheticTestEvents() {
        assertFalse(
            GameWebhookClient.isEligiblePayment(
                payment(
                    reference = "TEST-REF-55739",
                    body = "Test payment webhook: You have received ETB 250.00. Ref: TEST-REF-55739."
                )
            )
        )
    }

    @Test
    fun rejectsDebitNonTelebirrAndWrongAmountReceipts() {
        assertFalse(GameWebhookClient.isEligiblePayment(payment(type = "Debit")))
        assertFalse(GameWebhookClient.isEligiblePayment(payment(sender = "Bank")))
        assertFalse(GameWebhookClient.isEligiblePayment(payment(amount = 200.0)))
        assertFalse(GameWebhookClient.isEligiblePayment(payment(currency = "USD")))
        assertFalse(GameWebhookClient.isEligiblePayment(payment(amount = 250.5)))
    }
}
