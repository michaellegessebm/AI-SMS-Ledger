package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RemoteDatabaseConfigEntity
import com.example.data.remote.RemoteDatabaseSyncManager
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

@Composable
fun CloudDatabaseScreen(
    config: RemoteDatabaseConfigEntity?,
    isTestingConnection: Boolean,
    isSyncingToCloud: Boolean,
    testResult: String?,
    isTestingGameWebhook: Boolean,
    gameWebhookTestResult: String?,
    onSaveConfig: (type: String, url: String, apiKey: String, path: String, isAutoSync: Boolean) -> Unit,
    onTestConnection: (type: String, url: String, apiKey: String, path: String) -> Unit,
    onSyncAll: () -> Unit,
    onSaveGameWebhook: (enabled: Boolean, url: String, token: String) -> Unit,
    onTestGameWebhook: (url: String, token: String) -> Unit,
    onRetryGameWebhooks: () -> Unit
) {
    var selectedType by remember(config) {
        mutableStateOf(config?.databaseType ?: "NEON_POSTGRES")
    }
    var databaseUrlInput by remember(config) {
        mutableStateOf(config?.databaseUrl.orEmpty())
    }
    var apiKeyInput by remember(config) {
        mutableStateOf(config?.apiKeyOrToken.orEmpty())
    }
    var pathInput by remember(config) {
        mutableStateOf(config?.tableNameOrPath ?: "transactions")
    }
    var autoSyncEnabled by remember(config) {
        mutableStateOf(config?.isAutoSyncEnabled ?: true)
    }
    var gameWebhookUrlInput by remember(config) {
        mutableStateOf(
            config?.gameWebhookUrl?.ifBlank {
                "https://tiktok-live-game.replit.app/api/webhooks/sms-payment"
            } ?: "https://tiktok-live-game.replit.app/api/webhooks/sms-payment"
        )
    }
    var gameWebhookTokenInput by remember(config) {
        mutableStateOf(config?.gameWebhookToken ?: "")
    }
    var gameWebhookEnabled by remember(config) {
        mutableStateOf(config?.isGameWebhookEnabled ?: false)
    }

    // Auto-detect PostgreSQL connection string paste
    val postgresParsed = remember(databaseUrlInput) {
        RemoteDatabaseSyncManager.parsePostgresUri(databaseUrlInput)
    }

    LaunchedEffect(postgresParsed) {
        postgresParsed?.let { parsed ->
            selectedType = "NEON_POSTGRES"
            if (apiKeyInput.isBlank()) {
                apiKeyInput = parsed.password
            }
        }
    }

    val providers = listOf(
        Pair("NEON_POSTGRES", "Neon PostgreSQL"),
        Pair("FIREBASE", "Firebase DB / REST"),
        Pair("SUPABASE", "Supabase REST"),
        Pair("CUSTOM_REST", "Custom API / Webhook")
    )

    fun applyPreset(type: String) {
        selectedType = type
        when (type) {
            "NEON_POSTGRES" -> {
                if (databaseUrlInput.isBlank() || databaseUrlInput.contains("firebase")) {
                    databaseUrlInput = ""
                    apiKeyInput = ""
                }
            }
            "FIREBASE" -> {
                if (databaseUrlInput.isBlank() || databaseUrlInput.contains("neon") || databaseUrlInput.contains("supabase")) {
                    databaseUrlInput = "https://your-app-default-rtdb.firebaseio.com/transactions.json"
                }
            }
            "SUPABASE" -> {
                if (databaseUrlInput.isBlank() || databaseUrlInput.contains("firebase")) {
                    databaseUrlInput = "https://your-project.supabase.co/rest/v1/transactions"
                }
            }
            "CUSTOM_REST" -> {
                if (databaseUrlInput.isBlank()) {
                    databaseUrlInput = "https://api.yourdomain.com/v1/sms-transactions"
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        // Cloud Status Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("cloud_status_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceLavender),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLOUD DATABASE SYNC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MediumText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )

                        Surface(
                            shape = CircleShape,
                            color = when (config?.lastSyncStatus) {
                                "CONNECTED", "SYNCED" -> CreditGreenContainer
                                "ERROR" -> DebitRedContainer
                                else -> VioletContainer
                            }
                        ) {
                            Text(
                                text = when (config?.lastSyncStatus) {
                                    "CONNECTED" -> "CONNECTED"
                                    "SYNCED" -> "ACTIVE SYNC"
                                    "ERROR" -> "SYNC ERROR"
                                    else -> "NOT CONFIGURED"
                                },
                                color = when (config?.lastSyncStatus) {
                                    "CONNECTED", "SYNCED" -> CreditGreen
                                    "ERROR" -> DebitRed
                                    else -> DarkVioletText
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = DeepVioletPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (config?.databaseUrl?.isNotBlank() == true && !config.databaseUrl.startsWith("https://your-project")) {
                                    "Target: ${config.databaseType}"
                                } else {
                                    "Neon PostgreSQL & Cloud DB"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = if (config?.databaseUrl?.isNotBlank() == true) {
                                    config.databaseUrl.take(38) + (if (config.databaseUrl.length > 38) "..." else "")
                                } else {
                                    "Paste your Neon connection string or HTTP endpoint"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MediumText,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Quick Sync All Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (autoSyncEnabled) "Auto-push on SMS arrival: ON" else "Auto-push: OFF",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumText,
                                fontSize = 11.sp
                            )
                        )

                        Button(
                            onClick = onSyncAll,
                            enabled = !isSyncingToCloud,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary),
                            modifier = Modifier.testTag("sync_all_button")
                        ) {
                            if (isSyncingToCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Push All Records", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Provider Selector Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Database Provider / Protocol:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MediumText
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { applyPreset(type) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepVioletPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = CardBackground,
                                labelColor = DarkText
                            )
                        )
                    }
                }
            }
        }

        // Configuration Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BorderOutline, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Database Connection Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    )

                    // Database URL / Connection String
                    OutlinedTextField(
                        value = databaseUrlInput,
                        onValueChange = { databaseUrlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("database_url_input"),
                        label = { Text("PostgreSQL URI or HTTP API Endpoint") },
                        placeholder = {
                            Text(
                                when (selectedType) {
                                    "NEON_POSTGRES" -> "postgresql://user:pass@ep-xyz.us-east-2.aws.neon.tech/neondb"
                                    "FIREBASE" -> "https://your-app-rtdb.firebaseio.com/transactions.json"
                                    "SUPABASE" -> "https://xyz.supabase.co/rest/v1/transactions"
                                    else -> "https://api.yourdomain.com/v1/transactions"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = DeepVioletPrimary)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepVioletPrimary,
                            unfocusedBorderColor = BorderOutline
                        )
                    )

                    // Detected Neon PostgreSQL banner
                    if (postgresParsed != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceLavender,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = DeepVioletPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Neon PostgreSQL connection detected! Ready to query ${postgresParsed.host} over secure HTTPS.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DarkVioletText,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // API Key / Auth Token (Optional/Required depending on DB)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        label = { Text("Password / Auth Token (Optional if in URI)") },
                        placeholder = { Text("e.g. Neon password, Supabase key, or Firebase secret") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = DeepVioletPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepVioletPrimary,
                            unfocusedBorderColor = BorderOutline
                        )
                    )

                    // Collection / Table name
                    OutlinedTextField(
                        value = pathInput,
                        onValueChange = { pathInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collection_path_input"),
                        label = { Text("PostgreSQL Table / Collection Name") },
                        placeholder = { Text("transactions") },
                        leadingIcon = {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = DeepVioletPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepVioletPrimary,
                            unfocusedBorderColor = BorderOutline
                        )
                    )

                    // Auto-sync switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-sync on incoming SMS",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkText
                                )
                            )
                            Text(
                                text = "Instantly insert parsed SMS transaction records into your database",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MediumText,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { autoSyncEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DeepVioletPrimary,
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = SurfaceLavender
                            )
                        )
                    }

                    // Test Connection Result Banner
                    testResult?.let { result ->
                        val isSuccess = result.startsWith("SUCCESS")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSuccess) CreditGreenContainer else DebitRedContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) CreditGreen else DebitRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = result.removePrefix("SUCCESS: ").removePrefix("FAILED: "),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSuccess) CreditGreen else DebitRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    // Action Buttons: Test Connection & Save
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onTestConnection(selectedType, databaseUrlInput, apiKeyInput, pathInput)
                            },
                            enabled = !isTestingConnection && databaseUrlInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("test_cloud_connection_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = DeepVioletPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = DeepVioletPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Test Connection",
                                    color = DeepVioletPrimary,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSaveConfig(selectedType, databaseUrlInput, apiKeyInput, pathInput, autoSyncEnabled)
                            },
                            enabled = databaseUrlInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_cloud_config_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary)
                        ) {
                            Text(
                                text = "Save Endpoint",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BorderOutline, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Game payment webhook",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    )
                    Text(
                        text = "This is separate from the database connection. After a verified ETB credit is saved, AI SMS Ledger sends its transaction number to the lottery game.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MediumText)
                    )
                    OutlinedTextField(
                        value = gameWebhookUrlInput,
                        onValueChange = { gameWebhookUrlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_webhook_url_input"),
                        label = { Text("Game webhook URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepVioletPrimary,
                            unfocusedBorderColor = BorderOutline
                        )
                    )
                    OutlinedTextField(
                        value = gameWebhookTokenInput,
                        onValueChange = { gameWebhookTokenInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_webhook_token_input"),
                        label = { Text("Shared webhook key") },
                        placeholder = { Text("Use the key configured in the game Replit Secrets") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepVioletPrimary,
                            unfocusedBorderColor = BorderOutline
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Send verified payments to the game",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkText
                                )
                            )
                            Text(
                                text = "Only ETB credit transactions with a reference are sent.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MediumText, fontSize = 11.sp)
                            )
                        }
                        Switch(
                            checked = gameWebhookEnabled,
                            onCheckedChange = { gameWebhookEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DeepVioletPrimary,
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = SurfaceLavender
                            )
                        )
                    }
                    gameWebhookTestResult?.let { result ->
                        val isSuccess = result.startsWith("SUCCESS")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSuccess) CreditGreenContainer else DebitRedContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.removePrefix("SUCCESS: ").removePrefix("FAILED: "),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSuccess) CreditGreen else DebitRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestGameWebhook(gameWebhookUrlInput, gameWebhookTokenInput) },
                            enabled = !isTestingGameWebhook
                                && gameWebhookUrlInput.isNotBlank()
                                && gameWebhookTokenInput.isNotBlank(),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline)
                        ) {
                            if (isTestingGameWebhook) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = DeepVioletPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Test webhook", color = DeepVioletPrimary)
                            }
                        }
                        Button(
                            onClick = {
                                onSaveGameWebhook(gameWebhookEnabled, gameWebhookUrlInput, gameWebhookTokenInput)
                            },
                            enabled = !gameWebhookEnabled
                                || (gameWebhookUrlInput.isNotBlank() && gameWebhookTokenInput.isNotBlank()),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary)
                        ) {
                            Text("Save webhook")
                        }
                    }
                    OutlinedButton(
                        onClick = onRetryGameWebhooks,
                        enabled = gameWebhookEnabled && !isSyncingToCloud,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline)
                    ) {
                        Text("Retry pending game events", color = DeepVioletPrimary)
                    }
                }
            }
        }

        // Schema helper card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceLavender.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PostgreSQL Table Columns Synced:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkVioletText
                        )
                    )
                    Text(
                        text = "• sender_number (VARCHAR)\n• merchant_name (VARCHAR)\n• amount (NUMERIC)\n• currency (VARCHAR)\n• transaction_number (VARCHAR)\n• transaction_type (VARCHAR)\n• date_time_string (VARCHAR)\n• date_time_millis (BIGINT)\n• raw_message_body (TEXT)\n• ai_summary (TEXT)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MediumText,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}
