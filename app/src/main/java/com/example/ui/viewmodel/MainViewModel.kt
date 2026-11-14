package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmsLedgerApplication
import com.example.ai.GeminiSmsParser
import com.example.data.model.RemoteDatabaseConfigEntity
import com.example.data.model.SenderFilterEntity
import com.example.data.model.SmsInboxItem
import com.example.data.model.TransactionEntity
import com.example.data.remote.GameWebhookClient
import com.example.util.NotificationHelper
import com.example.util.SmsInboxHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavTab(val label: String) {
    MONITOR("Monitor"),
    DATABASE("Database"),
    CLOUD_DB("Cloud DB"),
    SETUP("Senders"),
    INBOX("SMS Inbox")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SmsLedgerApplication).repository

    private val _currentTab = MutableStateFlow(AppNavTab.MONITOR)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _isSyncingToCloud = MutableStateFlow(false)
    val isSyncingToCloud: StateFlow<Boolean> = _isSyncingToCloud.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    private val _isTestingGameWebhook = MutableStateFlow(false)
    val isTestingGameWebhook: StateFlow<Boolean> = _isTestingGameWebhook.asStateFlow()

    private val _gameWebhookTestResult = MutableStateFlow<String?>(null)
    val gameWebhookTestResult: StateFlow<String?> = _gameWebhookTestResult.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _inboxMessages = MutableStateFlow<List<SmsInboxItem>>(emptyList())
    val inboxMessages: StateFlow<List<SmsInboxItem>> = _inboxMessages.asStateFlow()

    private val _isLoadingInbox = MutableStateFlow(false)
    val isLoadingInbox: StateFlow<Boolean> = _isLoadingInbox.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransaction: StateFlow<TransactionEntity?> = _selectedTransaction.asStateFlow()

    private val _showAddFilterDialog = MutableStateFlow(false)
    val showAddFilterDialog: StateFlow<Boolean> = _showAddFilterDialog.asStateFlow()

    val remoteConfig: StateFlow<RemoteDatabaseConfigEntity?> = repository.remoteConfig
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RemoteDatabaseConfigEntity(
                databaseType = "FIREBASE",
                databaseUrl = "https://your-project-id.firebaseio.com/transactions.json",
                tableNameOrPath = "transactions",
                isAutoSyncEnabled = true
            )
        )

    val allSenderFilters: StateFlow<List<SenderFilterEntity>> = repository.allFilters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSenderFilters: StateFlow<List<SenderFilterEntity>> = repository.activeFilters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _searchQuery,
        _typeFilter
    ) { transactions, query, type ->
        transactions.filter { txn ->
            val matchesQuery = query.isBlank() ||
                    txn.merchantOrName.contains(query, ignoreCase = true) ||
                    txn.transactionNumber.contains(query, ignoreCase = true) ||
                    txn.rawMessageBody.contains(query, ignoreCase = true) ||
                    txn.senderNumber.contains(query, ignoreCase = true)

            val matchesType = type == "ALL" ||
                    txn.transactionType.equals(type, ignoreCase = true) ||
                    (type == "DEBIT" && (txn.transactionType.contains("Debit", ignoreCase = true) || txn.transactionType.contains("Withdrawal", ignoreCase = true) || txn.transactionType.contains("POS", ignoreCase = true))) ||
                    (type == "CREDIT" && (txn.transactionType.contains("Credit", ignoreCase = true) || txn.transactionType.contains("Received", ignoreCase = true) || txn.transactionType.contains("Income", ignoreCase = true)))

            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val primaryCurrency: StateFlow<String> = repository.allTransactions.map { list ->
        list.firstOrNull { it.currency.isNotBlank() }?.currency ?: "ETB"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ETB")

    val totalIncome: StateFlow<Double> = repository.allTransactions.map { list ->
        list.filter { isIncomeType(it.transactionType) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = repository.allTransactions.map { list ->
        list.filter { isExpenseType(it.transactionType) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private fun isIncomeType(type: String): Boolean {
        val t = type.lowercase()
        return t.contains("credit") || t.contains("receive") || t.contains("income") || t.contains("deposit")
    }

    private fun isExpenseType(type: String): Boolean {
        val t = type.lowercase()
        return t.contains("debit") || t.contains("sent") || t.contains("pay") || t.contains("withdraw") ||
                t.contains("transfer") || t.contains("pos") || t.contains("charge") || t.contains("purchase") ||
                (!isIncomeType(type) && t.isNotBlank())
    }

    fun selectTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun selectTransaction(transaction: TransactionEntity?) {
        _selectedTransaction.value = transaction
    }

    fun setShowAddFilterDialog(show: Boolean) {
        _showAddFilterDialog.value = show
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearConnectionTestResult() {
        _connectionTestResult.value = null
    }

    fun saveRemoteConfig(
        databaseType: String,
        databaseUrl: String,
        apiKeyOrToken: String,
        tableNameOrPath: String,
        isAutoSync: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getRemoteConfigDirect() ?: RemoteDatabaseConfigEntity()
            val config = current.copy(
                databaseType = databaseType,
                databaseUrl = databaseUrl.trim(),
                apiKeyOrToken = apiKeyOrToken.trim(),
                tableNameOrPath = tableNameOrPath.trim().ifBlank { "transactions" },
                isAutoSyncEnabled = isAutoSync,
                lastSyncStatus = "CONFIGURED",
                lastSyncTime = System.currentTimeMillis()
            )
            repository.saveRemoteConfig(config)
            _statusMessage.value = "Remote database endpoint saved successfully!"
        }
    }

    fun saveGameWebhookConfig(enabled: Boolean, url: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getRemoteConfigDirect() ?: RemoteDatabaseConfigEntity()
            repository.saveRemoteConfig(
                current.copy(
                    isGameWebhookEnabled = enabled,
                    gameWebhookUrl = url.trim(),
                    gameWebhookToken = token.trim()
                )
            )
            _statusMessage.value = "Game webhook settings saved."
        }
    }

    fun testGameWebhook(url: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isTestingGameWebhook.value = true
            _gameWebhookTestResult.value = null
            val result = GameWebhookClient.testConnection(url, token)
            _isTestingGameWebhook.value = false
            _gameWebhookTestResult.value = if (result.isSuccess) {
                "SUCCESS: ${result.getOrNull()}"
            } else {
                "FAILED: ${result.exceptionOrNull()?.message ?: "Could not reach the game"}"
            }
        }
    }

    fun retryPendingGameWebhooks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingToCloud.value = true
            try {
                val result = repository.retryPendingGameWebhooks()
                _statusMessage.value = if (result.isSuccess) {
                    "Sent ${result.getOrDefault(0)} pending game webhook(s)."
                } else {
                    result.exceptionOrNull()?.message ?: "Could not retry game webhooks."
                }
            } catch (error: Exception) {
                _statusMessage.value = error.message ?: "Could not retry game webhooks."
            } finally {
                _isSyncingToCloud.value = false
            }
        }
    }

    fun testCloudConnection(
        databaseType: String,
        databaseUrl: String,
        apiKeyOrToken: String,
        tableNameOrPath: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _connectionTestResult.value = null
            _isTestingConnection.value = true
            val config = RemoteDatabaseConfigEntity(
                id = 1,
                databaseType = databaseType,
                databaseUrl = databaseUrl.trim(),
                apiKeyOrToken = apiKeyOrToken.trim(),
                tableNameOrPath = tableNameOrPath.trim().ifBlank { "transactions" }
            )
            val result = repository.testRemoteConnection(config)
            _isTestingConnection.value = false

            if (result.isSuccess) {
                _connectionTestResult.value = "SUCCESS: " + result.getOrNull()
                _statusMessage.value = "Remote DB Connected!"
            } else {
                _connectionTestResult.value = "FAILED: " + (result.exceptionOrNull()?.message ?: "Unknown error")
                _statusMessage.value = "Connection test failed."
            }
        }
    }

    fun syncAllTransactionsToRemote() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingToCloud.value = true
            _statusMessage.value = "Syncing transactions to remote database..."

            val result = repository.syncAllPendingTransactions()
            _isSyncingToCloud.value = false

            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _statusMessage.value = "Synced $count transaction(s) to remote database!"
            } else {
                _statusMessage.value = "Sync failed: " + (result.exceptionOrNull()?.message ?: "Check URL & credentials")
            }
        }
    }

    fun syncSingleTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingToCloud.value = true
            _statusMessage.value = "Pushing to remote DB..."

            val result = repository.syncTransactionToRemote(transaction)
            _isSyncingToCloud.value = false

            if (result.isSuccess) {
                _statusMessage.value = "Synced transaction #${transaction.transactionNumber} to remote DB!"
                _selectedTransaction.value = transaction.copy(remoteSyncStatus = "SYNCED")
            } else {
                _statusMessage.value = "Remote sync error: " + result.exceptionOrNull()?.message
            }
        }
    }

    fun addSenderFilter(sender: String, label: String) {
        if (sender.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val filter = SenderFilterEntity(
                phoneNumberOrSenderId = sender.trim(),
                label = label.trim().ifBlank { "Monitored Sender" },
                isEnabled = true,
                autoAiParse = true
            )
            repository.insertFilter(filter)
            _statusMessage.value = "Added sender rule: ${filter.phoneNumberOrSenderId}"
        }
    }

    fun toggleFilterEnabled(filter: SenderFilterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFilter(filter.copy(isEnabled = !filter.isEnabled))
        }
    }

    fun deleteFilter(filter: SenderFilterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFilter(filter)
            _statusMessage.value = "Removed sender rule: ${filter.phoneNumberOrSenderId}"
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
            if (_selectedTransaction.value?.id == transaction.id) {
                _selectedTransaction.value = null
            }
            _statusMessage.value = "Deleted transaction record"
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllTransactions()
            _statusMessage.value = "Database cleared"
        }
    }

    fun loadDeviceInbox() {
        viewModelScope.launch {
            _isLoadingInbox.value = true
            val items = SmsInboxHelper.readDeviceInboxSms(getApplication())
            _inboxMessages.value = items
            _isLoadingInbox.value = false
        }
    }

    fun toggleSmsSelection(item: SmsInboxItem) {
        _inboxMessages.value = _inboxMessages.value.map {
            if (it.id == item.id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAllInbox(select: Boolean) {
        _inboxMessages.value = _inboxMessages.value.map { it.copy(isSelected = select) }
    }

    fun extractSelectedSmsWithAi() {
        val selected = _inboxMessages.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _statusMessage.value = "No messages selected"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isAiProcessing.value = true
            _statusMessage.value = "AI extracting ${selected.size} selected message(s)..."

            var count = 0
            for (sms in selected) {
                try {
                    val parsed = GeminiSmsParser.parseSmsMessage(sms.address, sms.body, sms.date)
                    val entity = TransactionEntity(
                        senderNumber = sms.address,
                        merchantOrName = parsed.name,
                        amount = parsed.amount,
                        currency = parsed.currency,
                        transactionNumber = parsed.transactionNumber,
                        dateTimeMillis = parsed.dateTimeMillis,
                        dateTimeString = parsed.dateTimeString,
                        transactionType = parsed.transactionType,
                        rawMessageBody = sms.body,
                        aiSummary = parsed.summary,
                        isAutoTriggered = false,
                        remoteSyncStatus = "PENDING",
                        createdAt = System.currentTimeMillis()
                    )
                    val id = repository.insertTransaction(entity)
                    val savedEntity = entity.copy(id = id)

                    // Push to remote DB
                    repository.syncTransactionToRemote(savedEntity)
                    count++
                } catch (e: Exception) {
                    // Continue to next message
                }
            }

            _isAiProcessing.value = false
            _statusMessage.value = "Extracted and saved $count transaction(s) to DB!"
            _inboxMessages.value = _inboxMessages.value.map { it.copy(isSelected = false) }
        }
    }

    fun simulateIncomingSms(sender: String, messageBody: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAiProcessing.value = true
            _statusMessage.value = "Receiving simulated SMS from $sender..."

            val isMonitored = repository.isSenderMonitored(sender)
            val activeFiltersList = repository.getActiveFiltersList()

            if (activeFiltersList.isNotEmpty() && !isMonitored) {
                _isAiProcessing.value = false
                _statusMessage.value = "SMS ignored: Sender $sender is not in active monitoring filter list."
                return@launch
            }

            _statusMessage.value = "Sender matched! Extracting transaction with AI..."
            val parsed = GeminiSmsParser.parseSmsMessage(sender, messageBody, System.currentTimeMillis())

            val entity = TransactionEntity(
                senderNumber = sender,
                merchantOrName = parsed.name,
                amount = parsed.amount,
                currency = parsed.currency,
                transactionNumber = parsed.transactionNumber,
                dateTimeMillis = parsed.dateTimeMillis,
                dateTimeString = parsed.dateTimeString,
                transactionType = parsed.transactionType,
                rawMessageBody = messageBody,
                aiSummary = parsed.summary,
                isAutoTriggered = true,
                remoteSyncStatus = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            val id = repository.insertTransaction(entity)
            var savedEntity = entity.copy(id = id)

            // Auto push to remote database
            try {
                val syncResult = repository.syncTransactionToRemote(savedEntity)
                if (syncResult.isSuccess) {
                    savedEntity = savedEntity.copy(remoteSyncStatus = "SYNCED")
                }
            } catch (e: Exception) {
                // local record is still preserved
            }

            NotificationHelper.showTransactionNotification(getApplication(), savedEntity)

            _isAiProcessing.value = false
            _statusMessage.value = "Transaction saved & synced: ${savedEntity.merchantOrName} (${savedEntity.currency} ${savedEntity.amount})"
        }
    }
}
