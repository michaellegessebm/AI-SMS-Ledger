package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionEntity
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenContainer
import com.example.ui.theme.DarkText
import com.example.ui.theme.DarkVioletText
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedContainer
import com.example.ui.theme.DeepVioletPrimary
import com.example.ui.theme.MediumText
import com.example.ui.theme.MutedText
import com.example.ui.theme.SurfaceLavender
import com.example.ui.theme.VioletContainer

@Composable
fun TransactionDetailsDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onSyncToCloud: ((TransactionEntity) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderOutline, RoundedCornerShape(24.dp))
                .testTag("transaction_details_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRANSACTION DETAILS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DeepVioletPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = transaction.merchantOrName.ifBlank { "Parsed SMS Record" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MediumText
                        )
                    }
                }

                // Amount Highlight Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceLavender,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${transaction.currency} ${String.format("%.2f", transaction.amount)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkVioletText
                            )
                        )
                        Text(
                            text = transaction.transactionType,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = DeepVioletPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Cloud Database Sync Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (transaction.remoteSyncStatus) {
                        "SYNCED" -> CreditGreenContainer
                        "FAILED" -> DebitRedContainer
                        else -> VioletContainer
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (transaction.remoteSyncStatus) {
                                    "SYNCED" -> Icons.Default.CloudDone
                                    "FAILED" -> Icons.Default.CloudOff
                                    else -> Icons.Default.CloudUpload
                                },
                                contentDescription = null,
                                tint = when (transaction.remoteSyncStatus) {
                                    "SYNCED" -> CreditGreen
                                    "FAILED" -> DebitRed
                                    else -> DeepVioletPrimary
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (transaction.remoteSyncStatus) {
                                    "SYNCED" -> "Synced to Remote Database"
                                    "FAILED" -> "Remote Sync Failed"
                                    else -> "Pending Cloud Sync"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when (transaction.remoteSyncStatus) {
                                        "SYNCED" -> CreditGreen
                                        "FAILED" -> DebitRed
                                        else -> DarkVioletText
                                    }
                                )
                            )
                        }

                        if (onSyncToCloud != null && transaction.remoteSyncStatus != "SYNCED") {
                            Text(
                                text = "Push Now",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DeepVioletPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable { onSyncToCloud(transaction) }
                            )
                        }
                    }
                }

                // Extracted Fields Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(
                        label = "Transaction Number / Ref ID",
                        value = "#${transaction.transactionNumber.ifBlank { "N/A" }}",
                        isMonospace = true,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(transaction.transactionNumber))
                        }
                    )

                    DetailRow(
                        label = "Sender Phone / Address",
                        value = transaction.senderNumber
                    )

                    DetailRow(
                        label = "Date & Time",
                        value = transaction.dateTimeString.ifBlank { "N/A" }
                    )

                    if (transaction.aiSummary.isNotBlank()) {
                        DetailRow(
                            label = "AI Extracted Summary",
                            value = transaction.aiSummary
                        )
                    }

                    if (transaction.remoteSyncError != null) {
                        DetailRow(
                            label = "Sync Error Details",
                            value = transaction.remoteSyncError
                        )
                    }
                }

                // Raw SMS Message Body
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "RAW INCOMING SMS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MutedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF7F2FA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = transaction.rawMessageBody,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DarkText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Action Buttons (Delete & Close)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDelete(transaction)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DebitRed)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = DebitRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = DebitRed)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
                ),
                modifier = Modifier.weight(1f)
            )

            if (onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = DeepVioletPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
