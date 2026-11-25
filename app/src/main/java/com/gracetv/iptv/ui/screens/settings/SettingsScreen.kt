package com.gracetv.iptv.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.gracetv.iptv.domain.model.AspectRatioMode
import com.gracetv.iptv.domain.model.ThemeMode
import com.gracetv.iptv.ui.components.ParentalPinPromptDialog
import com.gracetv.iptv.ui.components.ParentalPinSetupDialog
import com.gracetv.iptv.ui.theme.GraceGreen
import com.gracetv.iptv.ui.theme.GraceRed
import com.gracetv.iptv.ui.theme.GraceYellow
import com.gracetv.iptv.ui.theme.GraceYellowContainer
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSupport: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val scope = rememberCoroutineScope()

    var showPinSetup by remember { mutableStateOf(false) }
    var showPinVerifyToDisable by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var keywordsInput by remember(prefs.restrictedKeywords) {
        mutableStateOf(prefs.restrictedKeywords.joinToString(", "))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        state.statusMessage?.let { msg ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GraceGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = GraceGreen, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::clearMessage) { Text("OK") }
                    }
                }
            }
        }

        // General Settings
        item {
            SettingsCard("General Settings") {
                Text("App Theme", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GraceYellowContainer,
                                selectedLabelColor = GraceYellow
                            )
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Language Preference", style = MaterialTheme.typography.titleSmall)
                val langs = listOf("en" to "English", "es" to "Español", "ur" to "اردو", "ar" to "العربية")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    langs.forEach { (code, label) ->
                        FilterChip(
                            selected = prefs.languageCode == code,
                            onClick = { viewModel.setLanguageCode(code) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GraceYellowContainer,
                                selectedLabelColor = GraceYellow
                            )
                        )
                    }
                }

                if (state.playlists.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Default Playlist", style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.playlists.forEach { pl ->
                            FilterChip(
                                selected = pl.isDefault,
                                onClick = { viewModel.setDefaultPlaylist(pl.id) },
                                label = { Text("${pl.name} (${pl.channelCount} ch)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GraceYellowContainer,
                                    selectedLabelColor = GraceYellow
                                )
                            )
                        }
                    }
                }

                SettingsSwitchRow(
                    title = "Auto-start Last Channel",
                    subtitle = "Automatically launch the last watched Live TV channel on startup",
                    checked = prefs.autoStartLastChannel,
                    onCheckedChange = viewModel::setAutoStartLastChannel
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = viewModel::refreshActivePlaylist,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(3.dp))
                        Text("Refresh Playlist")
                    }
                    OutlinedButton(
                        onClick = viewModel::clearCache,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = GraceRed)
                        Spacer(Modifier.padding(3.dp))
                        Text("Clear Cache", color = GraceRed)
                    }
                }
            }
        }

        // Player Settings
        item {
            SettingsCard("Player Settings") {
                Text("Default Aspect Ratio", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(AspectRatioMode.FIT, AspectRatioMode.FILL, AspectRatioMode.ZOOM).forEach { mode ->
                        FilterChip(
                            selected = prefs.defaultAspectRatio == mode,
                            onClick = { viewModel.setDefaultAspectRatio(mode) },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GraceYellowContainer,
                                selectedLabelColor = GraceYellow
                            )
                        )
                    }
                }

                SettingsSwitchRow(
                    title = "Auto Landscape Playback",
                    subtitle = "Automatically switch player screen to landscape orientation",
                    checked = prefs.autoLandscape,
                    onCheckedChange = viewModel::setAutoLandscape
                )
                SettingsSwitchRow(
                    title = "Hardware Video Decoding",
                    subtitle = "Prefer device hardware MediaCodec decoders when supported",
                    checked = prefs.hardwareDecoding,
                    onCheckedChange = viewModel::setHardwareDecoding
                )
                SettingsSwitchRow(
                    title = "Show Playback Controls on Start",
                    subtitle = "Display player overlay controls briefly when playback begins",
                    checked = prefs.showPlaybackControlsOnStart,
                    onCheckedChange = viewModel::setShowControlsOnStart
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prefs.preferredAudioLanguage,
                        onValueChange = viewModel::setPreferredAudioLanguage,
                        label = { Text("Audio Lang") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prefs.preferredSubtitleLanguage,
                        onValueChange = viewModel::setPreferredSubtitleLanguage,
                        label = { Text("Subtitle Lang") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Parental Controls
        item {
            SettingsCard("Parental Controls") {
                SettingsSwitchRow(
                    title = "Enable Parental Lock",
                    subtitle = if (prefs.hasParentalPin) {
                        "PIN is configured and restricts matching categories"
                    } else {
                        "Set a PIN to protect restricted categories and channels"
                    },
                    checked = prefs.parentalEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (!prefs.hasParentalPin) {
                                showPinSetup = true
                            } else {
                                viewModel.setParentalEnabled(true)
                            }
                        } else {
                            if (prefs.hasParentalPin) {
                                pinError = null
                                showPinVerifyToDisable = true
                            } else {
                                viewModel.setParentalEnabled(false)
                            }
                        }
                    }
                )

                Button(
                    onClick = { showPinSetup = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GraceRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("configure_pin_button")
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(if (prefs.hasParentalPin) "Change Parental PIN" else "Set Parental PIN")
                }

                OutlinedTextField(
                    value = keywordsInput,
                    onValueChange = { keywordsInput = it },
                    label = { Text("Restricted Category Keywords (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { viewModel.setRestrictedKeywords(keywordsInput) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Restriction Keywords", color = GraceYellow)
                }
            }
        }

        // About Grace TV & Support
        item {
            SettingsCard("About Grace TV") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Application", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Grace TV", style = MaterialTheme.typography.titleSmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.labelMedium, color = GraceGreen)
                }
                Text(
                    "Grace TV is an independent IPTV media player supporting user-provided Xtream Codes, M3U/M3U8, and XMLTV playlists with zero ads or subscriptions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null)
                        Spacer(Modifier.padding(3.dp))
                        Text("Privacy Policy")
                    }
                    Button(
                        onClick = onOpenSupport,
                        colors = ButtonDefaults.buttonColors(containerColor = GraceGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_support_button")
                    ) {
                        Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary)
                        Spacer(Modifier.padding(3.dp))
                        Text("Support", color = MaterialTheme.colorScheme.onTertiary)
                    }
                }
            }
        }
    }

    if (showPinSetup) {
        ParentalPinSetupDialog(
            onDismiss = { showPinSetup = false },
            onSavePin = { pin ->
                viewModel.saveParentalPin(pin)
                showPinSetup = false
            }
        )
    }

    if (showPinVerifyToDisable) {
        ParentalPinPromptDialog(
            title = "Disable Parental Lock",
            subtitle = "Enter your current PIN to disable parental restrictions.",
            errorMessage = pinError,
            onDismiss = { showPinVerifyToDisable = false },
            onSubmitPin = { pin ->
                scope.launch {
                    if (viewModel.verifyParentalPin(pin)) {
                        viewModel.setParentalEnabled(false)
                        showPinVerifyToDisable = false
                    } else {
                        pinError = "Incorrect PIN."
                    }
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Grace TV Privacy Notice") },
            text = {
                Text(
                    "Grace TV stores your playlists, favorites, watch history, and EPG data strictly on your device. " +
                        "Sensitive playlist credentials are encrypted locally using Android Keystore. " +
                        "Grace TV does not include advertisements, analytics trackers, or bundled media streams."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = GraceYellow)
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GraceGreen,
                checkedTrackColor = GraceGreen.copy(alpha = 0.35f)
            )
        )
    }
}
