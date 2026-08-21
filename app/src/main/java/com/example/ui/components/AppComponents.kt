package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.theme.ActiveIndicatorGreen
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
import com.example.ui.viewmodel.AppNavTab

@Composable
fun AppHeader(
    onOpenCloudDb: () -> Unit = {},
    onScanInbox: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VioletContainer)
                    .clickable { onOpenCloudDb() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "App Shield Icon",
                    tint = DarkVioletText,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = "AI SMS Ledger",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Live SMS & Cloud DB Sync",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MediumText
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Scan Inbox button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceLavender)
                    .clickable { onScanInbox() }
                    .testTag("header_inbox_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = "Scan SMS Inbox",
                    tint = DeepVioletPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Cloud DB button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceLavender)
                    .clickable { onOpenCloudDb() }
                    .testTag("header_cloud_db_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Cloud Database Sync",
                    tint = DeepVioletPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Pulsating AI status indicator badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DeepVioletPrimary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ActiveIndicatorGreen)
                        .alpha(alpha)
                )
            }
        }
    }
}

@Composable
fun CleanLiveStatusCard(
    activeFilterCount: Int,
    targetSenderPreview: String,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onConfigureClick() }
            .testTag("live_status_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceLavender),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ActiveIndicatorGreen)
                            .alpha(alpha)
                    )
                    Text(
                        text = "LIVE MONITORING ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MediumText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = VioletContainer
                ) {
                    Text(
                        text = "AI TRIGGER ON",
                        color = DarkVioletText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (targetSenderPreview.isNotBlank()) targetSenderPreview else "All Target Senders Monitored",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        letterSpacing = (-0.5).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "AI reads Name, Amount, TXN ID & Date → auto-syncs to Database",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MediumText,
                        fontSize = 13.sp
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$activeFilterCount Active Filter Rules",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MediumText,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = "Configure Rules & Cloud DB →",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = DeepVioletPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun CleanTransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderOutline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("transaction_card_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Transaction Number & Relative Time + Cloud Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TXN: #${transaction.transactionNumber.ifBlank { "N/A" }}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MediumText,
                            fontSize = 11.sp
                        )
                    )

                    // Cloud Sync Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (transaction.remoteSyncStatus) {
                            "SYNCED" -> CreditGreenContainer
                            "FAILED" -> DebitRedContainer
                            else -> VioletContainer.copy(alpha = 0.5f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = when (transaction.remoteSyncStatus) {
                                    "SYNCED" -> "Cloud DB"
                                    "FAILED" -> "Sync Error"
                                    else -> "Pending"
                                },
                                color = when (transaction.remoteSyncStatus) {
                                    "SYNCED" -> CreditGreen
                                    "FAILED" -> DebitRed
                                    else -> DarkVioletText
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Text(
                    text = formatRelativeTime(transaction.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutedText,
                        fontSize = 10.sp
                    )
                )
            }

            // 2-Column Grid of 4 Key Extracted Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Name and Amount
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(
                            text = "COUNTERPARTY / NAME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = transaction.merchantOrName.ifBlank { "Transaction Alert" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkText,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column {
                        Text(
                            text = "AMOUNT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${if (transaction.currency == "USD") "$" else "${transaction.currency} "}${String.format("%.2f", transaction.amount)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkText,
                                fontSize = 20.sp
                            )
                        )
                    }
                }

                // Right Column: Type and Time
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(
                            text = "TYPE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = transaction.transactionType.ifBlank { "Debit" },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DarkText,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column {
                        Text(
                            text = "TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = transaction.dateTimeString.ifBlank { "Just now" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DarkText,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            // Raw summary / AI badge container with clean layout & wrap
            if (transaction.aiSummary.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceLavender.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = transaction.aiSummary,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumText,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        if (transaction.isAutoTriggered) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = VioletContainer
                            ) {
                                Text(
                                    text = "Auto AI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DeepVioletPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanBottomNav(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = Color(0xFFF3EDF7),
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                label = "Monitor",
                icon = { Icon(Icons.Default.Phone, contentDescription = "Monitor") },
                isSelected = currentTab == AppNavTab.MONITOR,
                onClick = { onTabSelected(AppNavTab.MONITOR) }
            )

            NavTabItem(
                label = "Database",
                icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Database") },
                isSelected = currentTab == AppNavTab.DATABASE,
                onClick = { onTabSelected(AppNavTab.DATABASE) }
            )

            NavTabItem(
                label = "Cloud DB",
                icon = { Icon(Icons.Default.CloudSync, contentDescription = "Cloud Database") },
                isSelected = currentTab == AppNavTab.CLOUD_DB,
                onClick = { onTabSelected(AppNavTab.CLOUD_DB) }
            )

            NavTabItem(
                label = "Senders",
                icon = { Icon(Icons.Default.Settings, contentDescription = "Senders") },
                isSelected = currentTab == AppNavTab.SETUP,
                onClick = { onTabSelected(AppNavTab.SETUP) }
            )

            NavTabItem(
                label = "Inbox",
                icon = { Icon(Icons.Default.Inbox, contentDescription = "SMS Inbox") },
                isSelected = currentTab == AppNavTab.INBOX,
                onClick = { onTabSelected(AppNavTab.INBOX) }
            )
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLavender)
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        } else {
            Box(
                modifier = Modifier.padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) DarkText else MediumText
            )
        )
    }
}

fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        else -> "${days}d ago"
    }
}
