package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.TransactionEntity
import com.example.ui.components.CleanTransactionCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        val sampleTxn = TransactionEntity(
            senderNumber = "+1 (555) 012-3456",
            merchantOrName = "Chase Bank Alerts",
            amount = 1240.00,
            currency = "USD",
            transactionNumber = "TXN-88219",
            dateTimeString = "14:32 PM",
            transactionType = "Withdrawal",
            rawMessageBody = "Chase Alert: You made a withdrawal of $1,240.00 at ATM #4029. Ref #TXN-88219.",
            aiSummary = "ATM withdrawal"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                CleanTransactionCard(transaction = sampleTxn)
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
