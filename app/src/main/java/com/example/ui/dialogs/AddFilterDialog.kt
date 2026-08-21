package com.example.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderOutline
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkText
import com.example.ui.theme.DeepVioletPrimary
import com.example.ui.theme.MediumText
import com.example.ui.theme.MutedText

@Composable
fun AddFilterDialog(
    onDismiss: () -> Unit,
    onConfirm: (sender: String, label: String) -> Unit
) {
    var senderInput by remember { mutableStateOf("") }
    var labelInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderOutline, RoundedCornerShape(24.dp))
                .testTag("add_filter_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Target Sender / Number",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MediumText
                        )
                    }
                }

                Text(
                    text = "Specify the exact phone number (e.g. +15550123456) or alphanumeric sender ID (e.g. Chase, Telebirr, CBEBirr) that should trigger AI parsing.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MediumText)
                )

                // Phone number / Sender ID field
                OutlinedTextField(
                    value = senderInput,
                    onValueChange = {
                        senderInput = it
                        if (isError) isError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sender_input_field"),
                    label = { Text("Phone Number or Sender ID") },
                    placeholder = { Text("e.g. +1 555 012-3456 or Chase") },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Sender number or ID cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepVioletPrimary,
                        unfocusedBorderColor = BorderOutline
                    )
                )

                // Label field
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sender_label_field"),
                    label = { Text("Label / Description (Optional)") },
                    placeholder = { Text("e.g. Chase Bank Alerts") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepVioletPrimary,
                        unfocusedBorderColor = BorderOutline
                    )
                )

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = MediumText)
                    }

                    Button(
                        onClick = {
                            if (senderInput.isBlank()) {
                                isError = true
                            } else {
                                onConfirm(senderInput.trim(), labelInput.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_filter_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepVioletPrimary)
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    }
}
