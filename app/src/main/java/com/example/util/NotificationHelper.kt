package com.example.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.SmsLedgerApplication
import com.example.data.model.TransactionEntity

object NotificationHelper {
    fun showTransactionNotification(context: Context, transaction: TransactionEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            transaction.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountFormatted = if (transaction.amount > 0) {
            "${transaction.currency} ${String.format("%.2f", transaction.amount)}"
        } else {
            "New SMS Record"
        }

        val title = "SMS AI: ${transaction.transactionType} ($amountFormatted)"
        val bodyText = "From ${transaction.merchantOrName} • Ref: ${transaction.transactionNumber}\n${transaction.aiSummary}"

        val notification = NotificationCompat.Builder(context, SmsLedgerApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${transaction.rawMessageBody}\n\nExtracted: ${transaction.merchantOrName} | $amountFormatted | ${transaction.transactionNumber}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(transaction.id.toInt().takeIf { it != 0 } ?: (1000..9999).random(), notification)
    }
}
