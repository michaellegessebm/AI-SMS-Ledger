package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppHeader
import com.example.ui.components.CleanBottomNav
import com.example.ui.dialogs.AddFilterDialog
import com.example.ui.dialogs.TransactionDetailsDialog
import com.example.ui.screens.CloudDatabaseScreen
import com.example.ui.screens.DatabaseScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.DarkVioletText
import com.example.ui.theme.LavenderBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VioletContainerActive
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val activeFilters by viewModel.activeSenderFilters.collectAsStateWithLifecycle()
    val allFilters by viewModel.allSenderFilters.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val isSyncingToCloud by viewModel.isSyncingToCloud.collectAsStateWithLifecycle()
    val connectionTestResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val inboxMessages by viewModel.inboxMessages.collectAsStateWithLifecycle()
    val isLoadingInbox by viewModel.isLoadingInbox.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val showAddFilterDialog by viewModel.showAddFilterDialog.collectAsStateWithLifecycle()
    val remoteConfig by viewModel.remoteConfig.collectAsStateWithLifecycle()

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val primaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LavenderBg,
        topBar = {
            AppHeader(
                onOpenCloudDb = { viewModel.selectTab(AppNavTab.CLOUD_DB) },
                onScanInbox = { viewModel.selectTab(AppNavTab.INBOX) }
            )
        },
        bottomBar = {
            CleanBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (currentTab) {
                        AppNavTab.SETUP -> viewModel.setShowAddFilterDialog(true)
                        AppNavTab.INBOX -> {
                            if (inboxMessages.any { it.isSelected }) {
                                viewModel.extractSelectedSmsWithAi()
                            } else {
                                viewModel.loadDeviceInbox()
                            }
                        }
                        AppNavTab.CLOUD_DB -> viewModel.syncAllTransactionsToRemote()
                        AppNavTab.MONITOR -> viewModel.setShowAddFilterDialog(true)
                        AppNavTab.DATABASE -> viewModel.setShowAddFilterDialog(true)
                    }
                },
                containerColor = VioletContainerActive,
                contentColor = DarkVioletText,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp, end = 4.dp)
                    .testTag("main_fab")
            ) {
                Icon(
                    imageVector = when (currentTab) {
                        AppNavTab.CLOUD_DB -> Icons.Default.CloudUpload
                        AppNavTab.INBOX -> if (inboxMessages.any { it.isSelected }) Icons.Default.AutoAwesome else Icons.Default.Inbox
                        else -> Icons.Default.Add
                    },
                    contentDescription = "Primary Action",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    AppNavTab.MONITOR -> {
                        MonitorScreen(
                            activeFilters = activeFilters,
                            transactions = transactions,
                            isAiProcessing = isAiProcessing,
                            onViewDatabase = { viewModel.selectTab(AppNavTab.DATABASE) },
                            onOpenSetup = { viewModel.selectTab(AppNavTab.SETUP) },
                            onOpenInbox = { viewModel.selectTab(AppNavTab.INBOX) },
                            onOpenCloudDb = { viewModel.selectTab(AppNavTab.CLOUD_DB) },
                            onTransactionClick = { viewModel.selectTransaction(it) },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) }
                        )
                    }

                    AppNavTab.DATABASE -> {
                        DatabaseScreen(
                            transactions = transactions,
                            searchQuery = searchQuery,
                            typeFilter = typeFilter,
                            totalCount = totalCount,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            currency = primaryCurrency,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onTypeFilterChange = { viewModel.setTypeFilter(it) },
                            onTransactionClick = { viewModel.selectTransaction(it) },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            onClearAll = { viewModel.clearAllTransactions() },
                            onOpenCloudDb = { viewModel.selectTab(AppNavTab.CLOUD_DB) }
                        )
                    }

                    AppNavTab.CLOUD_DB -> {
                        CloudDatabaseScreen(
                            config = remoteConfig,
                            isTestingConnection = isTestingConnection,
                            isSyncingToCloud = isSyncingToCloud,
                            testResult = connectionTestResult,
                            onSaveConfig = { type, url, apiKey, path, isAutoSync ->
                                viewModel.saveRemoteConfig(type, url, apiKey, path, isAutoSync)
                            },
                            onTestConnection = { type, url, apiKey, path ->
                                viewModel.testCloudConnection(type, url, apiKey, path)
                            },
                            onSyncAll = {
                                viewModel.syncAllTransactionsToRemote()
                            }
                        )
                    }

                    AppNavTab.SETUP -> {
                        SetupScreen(
                            filters = allFilters,
                            onAddFilterClick = { viewModel.setShowAddFilterDialog(true) },
                            onToggleFilter = { viewModel.toggleFilterEnabled(it) },
                            onDeleteFilter = { viewModel.deleteFilter(it) }
                        )
                    }

                    AppNavTab.INBOX -> {
                        InboxScreen(
                            inboxMessages = inboxMessages,
                            isLoading = isLoadingInbox,
                            isAiProcessing = isAiProcessing,
                            onLoadInbox = { viewModel.loadDeviceInbox() },
                            onToggleSelect = { viewModel.toggleSmsSelection(it) },
                            onSelectAll = { viewModel.selectAllInbox(it) },
                            onExtractSelected = { viewModel.extractSelectedSmsWithAi() }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddFilterDialog) {
        AddFilterDialog(
            onDismiss = { viewModel.setShowAddFilterDialog(false) },
            onConfirm = { sender, label -> viewModel.addSenderFilter(sender, label) }
        )
    }

    selectedTransaction?.let { transaction ->
        TransactionDetailsDialog(
            transaction = transaction,
            onDismiss = { viewModel.selectTransaction(null) },
            onDelete = { viewModel.deleteTransaction(it) },
            onSyncToCloud = { viewModel.syncSingleTransaction(it) }
        )
    }
}
