package com.gracetv.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow

@Composable
fun ParentalPinPromptDialog(
    title: String = "Parental Lock",
    subtitle: String = "Enter your 4-digit PIN to unlock restricted content.",
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmitPin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = GraceYellow) },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parental_pin_input")
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitPin(pin) },
                enabled = pin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                modifier = Modifier.testTag("parental_pin_confirm")
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ParentalPinSetupDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = GraceYellow) },
        title = { Text("Set Parental PIN", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Choose a 4 to 6 digit PIN to lock restricted categories and settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pin = it
                            localError = null
                        }
                    },
                    label = { Text("New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            confirmPin = it
                            localError = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_confirm_input")
                )
                if (localError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = localError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < 4 -> localError = "PIN must be at least 4 digits."
                        pin != confirmPin -> localError = "PINs do not match."
                        else -> onSavePin(pin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                modifier = Modifier.testTag("save_pin_button")
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
