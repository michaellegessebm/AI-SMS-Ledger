package com.example

import com.example.ai.GeminiSmsParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun telebirrSmsParsing_extractsAllFieldsAccurately() {
    val sampleMessage = """
      Dear Michael 
      You have received ETB 1.00 from Legesse Gebrehanna(2519****7054)  on 20/08/2026 22:24:21. Your transaction number is DHK115G4WD. Your current E-Money Account balance is ETB 92.08.
      Thank you for using telebirr
      Ethio telecom
    """.trimIndent()

    val result = GeminiSmsParser.extractWithLocalRegex("Telebirr", sampleMessage, 1787264661000L)

    assertEquals("DHK115G4WD", result.transactionNumber)
    assertEquals(1.00, result.amount, 0.001)
    assertEquals("ETB", result.currency)
    assertEquals("Credit", result.transactionType)
    assertEquals("Legesse Gebrehanna", result.name)
    assertEquals("20/08/2026 22:24:21", result.dateTimeString)
  }

  @Test
  fun transactionType_inflowOutflowCalculation_isAccurate() {
    fun isIncome(type: String): Boolean {
      val t = type.lowercase()
      return t.contains("credit") || t.contains("receive") || t.contains("income") || t.contains("deposit")
    }

    fun isExpense(type: String): Boolean {
      val t = type.lowercase()
      return t.contains("debit") || t.contains("sent") || t.contains("pay") || t.contains("withdraw") ||
              t.contains("transfer") || t.contains("pos") || t.contains("charge") || t.contains("purchase") ||
              (!isIncome(type) && t.isNotBlank())
    }

    val sampleTypes = listOf("Credit", "Debit", "Withdrawal", "POS Charge", "Payment", "Transfer", "Received")
    val amounts = listOf(100.0, 20.0, 15.0, 5.0, 30.0, 10.0, 50.0)

    val totalInflow = sampleTypes.zip(amounts).filter { isIncome(it.first) }.sumOf { it.second }
    val totalOutflow = sampleTypes.zip(amounts).filter { isExpense(it.first) }.sumOf { it.second }

    assertEquals(150.0, totalInflow, 0.001) // 100 + 50
    assertEquals(80.0, totalOutflow, 0.001)  // 20 + 15 + 5 + 30 + 10
  }
}

