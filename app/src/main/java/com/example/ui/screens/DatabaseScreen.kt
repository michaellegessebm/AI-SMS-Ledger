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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.CleanTransactionCard
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenContainer
import com.example.ui.theme.DarkText
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedContainer
import com.example.ui.theme.DeepVioletPrimary
import com.example.ui.theme.MediumText
import com.example.ui.theme.MutedText
import com.example.ui.theme.SurfaceLavender
import com.example.ui.theme.VioletContainer

@Composable
fun DatabaseScreen(
    transactions: List<TransactionEntity>,
    searchQuery: String,
    typeFilter: String,
    totalCount: Int,
    totalIncome: Double,
    totalExpense: Double,
    currency: String = "ETB",
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onClearAll: () -> Unit,
    onOpenCloudDb: (() -> Unit)? = null
) {
    val filterTypes = listOf("ALL", "DEBIT", "CREDIT", "Withdrawal", "POS Charge", "Payment", "Transfer")
    val currencyPrefix = if (currency.equals("USD", ignoreCase = true)) "$" else "$currency "

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Outflow / Debit summary
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = DebitRedContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = DebitRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TOTAL OUTFLOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DebitRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Text(
                        text = "$currencyPrefix${String.format("%.2f", totalExpense)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            // Inflow / Credit summary
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CreditGreenContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = CreditGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TOTAL INFLOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CreditGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Text(
                        text = "$currencyPrefix${String.format("%.2f", totalIncome)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("database_search_field"),
            placeholder = {
                Text(
                    text = "Search transactions, amounts, IDs...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MutedText)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = DeepVioletPrimary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MediumText
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = DeepVioletPrimary,
                unfocusedBorderColor = BorderOutline
            )
        )

        // Type Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterTypes.forEach { type ->
                val isSelected = typeFilter.equals(type, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeFilterChange(type) },
                    label = {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SurfaceLavender,
                        selectedLabelColor = DeepVioletPrimary,
                        containerColor = CardBackground,
                        labelColor = MediumText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) DeepVioletPrimary else BorderOutline,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        // Total result count and Clear Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${transactions.size} records found in database",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MediumText,
                    fontWeight = FontWeight.Medium
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onOpenCloudDb != null) {
                    TextButton(onClick = onOpenCloudDb) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = DeepVioletPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cloud DB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DeepVioletPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (transactions.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("clear_all_db_button")
                    ) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DebitRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Transactions List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matches for \"$searchQuery\"" else "Database is empty",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                        )
                        Text(
                            text = "New incoming SMS matching your target senders will appear here automatically and sync to your cloud DB.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumText
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(transactions, key = { it.id }) { transaction ->
                    CleanTransactionCard(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}
