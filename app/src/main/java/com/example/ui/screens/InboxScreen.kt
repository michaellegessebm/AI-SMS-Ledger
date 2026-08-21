package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.SmsInboxItem
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkText
import com.example.ui.theme.DarkVioletText
import com.example.ui.theme.DeepVioletPrimary
import com.example.ui.theme.MediumText
import com.example.ui.theme.MutedText
import com.example.ui.theme.SurfaceLavender
import com.example.ui.theme.VioletContainer

@Composable
fun InboxScreen(
    inboxMessages: List<SmsInboxItem>,
    isLoading: Boolean,
    isAiProcessing: Boolean,
    onLoadInbox: () -> Unit,
    onToggleSelect: (SmsInboxItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onExtractSelected: () -> Unit
) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            onLoadInbox()
        }
    }

    LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission) {
            onLoadInbox()
        }
    }

    val selectedCount = inboxMessages.count { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Permission Card if not granted
        if (!hasSmsPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceLavender),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Device SMS Inbox Access",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkVioletText
                        )
                    )
                    Text(
                        text = "Grant SMS read permission to browse your real device SMS messages, select banking/transaction alerts, and batch extract details with AI into the database.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MediumText)
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant SMS Permission")
                    }
                }
            }
        }

        // Selection & Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Checkbox(
                    checked = inboxMessages.isNotEmpty() && inboxMessages.all { it.isSelected },
                    onCheckedChange = { onSelectAll(it) },
                    colors = CheckboxDefaults.colors(checkedColor = DeepVioletPrimary)
                )
                Text(
                    text = if (selectedCount > 0) "$selectedCount selected" else "Select All",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onLoadInbox) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Inbox",
                        tint = DeepVioletPrimary
                    )
                }

                Button(
                    onClick = onExtractSelected,
                    enabled = selectedCount > 0 && !isAiProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary),
                    modifier = Modifier.testTag("extract_selected_button")
                ) {
                    if (isAiProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extracting...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract with AI ($selectedCount)", fontSize = 12.sp)
                    }
                }
            }
        }

        // List of Inbox messages
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DeepVioletPrimary)
                    }
                }
            } else if (inboxMessages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = DeepVioletPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No SMS Messages Found",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                            )
                            Text(
                                text = "When new SMS messages arrive on this device, tap Refresh to load and batch extract transaction details.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MediumText),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = onLoadInbox,
                                colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Refresh Inbox")
                            }
                        }
                    }
                }
            } else {
                items(inboxMessages, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (item.isSelected) DeepVioletPrimary else BorderOutline,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onToggleSelect(item) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isSelected) SurfaceLavender.copy(alpha = 0.4f) else CardBackground
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = item.isSelected,
                                onCheckedChange = { onToggleSelect(item) },
                                colors = CheckboxDefaults.colors(checkedColor = DeepVioletPrimary)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.address,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkText
                                        )
                                    )
                                    Text(
                                        text = formatRelativeTime(item.date),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MutedText,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MediumText,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
