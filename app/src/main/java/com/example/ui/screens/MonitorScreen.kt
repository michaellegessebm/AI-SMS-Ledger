package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.SenderFilterEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.CleanTransactionCard
import com.example.ui.theme.ActiveIndicatorGreen
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenContainer
import com.example.ui.theme.DarkText
import com.example.ui.theme.DarkVioletText
import com.example.ui.theme.DeepVioletPrimary
import com.example.ui.theme.MediumText
import com.example.ui.theme.MutedText
import com.example.ui.theme.SurfaceLavender
import com.example.ui.theme.VioletContainer

@Composable
fun MonitorScreen(
    activeFilters: List<SenderFilterEntity>,
    transactions: List<TransactionEntity>,
    isAiProcessing: Boolean,
    onViewDatabase: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenCloudDb: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current

    // Check actual Android system permissions
    var hasReceiveSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasReadSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionsToRequest = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasReceiveSmsPermission = result[Manifest.permission.RECEIVE_SMS] ?: hasReceiveSmsPermission
        hasReadSmsPermission = result[Manifest.permission.READ_SMS] ?: hasReadSmsPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = result[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
    }

    val allPermissionsGranted = hasReceiveSmsPermission && hasReadSmsPermission

    val primaryFilter = activeFilters.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        // Real Android System Permission Status Banner (if needed)
        if (!allPermissionsGranted) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("permission_prompt_card"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLavender),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = DeepVioletPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Real SMS Permissions Required",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkVioletText
                                )
                            )
                        }

                        Text(
                            text = "To allow the app to automatically detect incoming real SMS messages from your bank/phone numbers, extract details with AI, and push them to your cloud database, grant Android SMS permissions.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumText,
                                lineHeight = 18.sp
                            )
                        )

                        Button(
                            onClick = {
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("grant_sms_permissions_button")
                        ) {
                            Text("Grant Real SMS Permissions")
                        }
                    }
                }
            }
        }

        // Section: Active Live Monitoring Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("active_monitoring_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceLavender),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (allPermissionsGranted) ActiveIndicatorGreen else Color(0xFFE57373))
                            )
                            Text(
                                text = if (allPermissionsGranted) "SMS RECEIVER ACTIVE" else "PERMISSION PENDING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (allPermissionsGranted) DeepVioletPrimary else MediumText,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isAiProcessing) Color(0xFF6750A4) else DeepVioletPrimary
                        ) {
                            Text(
                                text = if (isAiProcessing) "AI EXTRACTING..." else "GEMINI AI READY",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Phone message badge icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = "SMS Monitoring",
                                tint = DeepVioletPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (activeFilters.isNotEmpty()) {
                                    "${activeFilters.size} Target Sender(s) Monitored"
                                } else {
                                    "All Incoming SMS Monitored"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DarkText
                                )
                            )
                            Text(
                                text = if (primaryFilter != null) {
                                    "E.g., ${primaryFilter.label} (${primaryFilter.phoneNumberOrSenderId})"
                                } else {
                                    "Extracting Name, Amount, TXN ID & Date automatically"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MediumText,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    // Rule count & quick edit link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${transactions.size} records in local database",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumText,
                                fontSize = 11.sp
                            )
                        )

                        TextButton(
                            onClick = { onOpenSetup() },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = "Manage Senders →",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = DeepVioletPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Real Actions Row (Import from Inbox & Cloud DB)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenInbox,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("scan_inbox_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepVioletPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Device SMS Inbox",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                OutlinedButton(
                    onClick = onOpenCloudDb,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("open_cloud_db_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DeepVioletPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DeepVioletPrimary
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Cloud DB Sync",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DeepVioletPrimary
                        )
                    )
                }
            }
        }

        // Section Header: Extracted Data
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extracted Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MediumText,
                        fontSize = 15.sp
                    )
                )

                Text(
                    text = "View All (${transactions.size}) →",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = DeepVioletPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onViewDatabase() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("view_database_link")
                )
            }
        }

        // Empty state or transaction list
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(VioletContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = DeepVioletPrimary
                            )
                        }
                        Text(
                            text = "Waiting for Incoming Bank SMS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        )
                        Text(
                            text = "When a real incoming SMS arrives from any monitored sender/bank, AI will automatically extract transaction details and sync to your database. You can also import existing SMS messages from your device inbox.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumText
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onOpenInbox,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Scan Device SMS Inbox")
                        }
                    }
                }
            }
        } else {
            // Show recent real transactions
            items(transactions.take(10), key = { it.id }) { transaction ->
                CleanTransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
            }
        }
    }
}
