package dev.scdeal.freskiel.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.scdeal.freskiel.sessions.SessionController
import dev.scdeal.freskiel.sessions.TransportPreference
import dev.scdeal.freskiel.ui.components.PiButton
import dev.scdeal.freskiel.ui.components.PiCard
import dev.scdeal.freskiel.ui.components.PiSpacing
import dev.scdeal.freskiel.ui.components.PiTopBar
import dev.scdeal.freskiel.ui.theme.ThemePreference
import kotlinx.coroutines.delay

@Composable
fun SettingsRoute(sessionController: SessionController) {
    val context = LocalContext.current
    val factory =
        remember(context, sessionController) {
            SettingsViewModelFactory(
                context = context,
                sessionController = sessionController,
            )
        }
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    var transientStatusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.messages.collect { message ->
            transientStatusMessage = message
        }
    }

    LaunchedEffect(transientStatusMessage) {
        if (transientStatusMessage != null) {
            delay(STATUS_MESSAGE_DURATION_MS)
            transientStatusMessage = null
        }
    }

    SettingsScreen(
        viewModel = settingsViewModel,
        transientStatusMessage = transientStatusMessage,
    )
}

@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    transientStatusMessage: String?,
) {
    val uiState = viewModel.uiState

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PiSpacing.md),
    ) {
        PiTopBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            actions = {},
        )

        ConnectionStatusCard(
            state = uiState,
            transientStatusMessage = transientStatusMessage,
            onPing = viewModel::pingBridge,
        )

        AgentAutomationCard(
            autoCompactionEnabled = uiState.autoCompactionEnabled,
            autoRetryEnabled = uiState.autoRetryEnabled,
            onToggleAutoCompaction = viewModel::toggleAutoCompaction,
            onToggleAutoRetry = viewModel::toggleAutoRetry,
        )

        TransportCard(
            transportPreference = uiState.transportPreference,
            effectiveTransportPreference = uiState.effectiveTransportPreference,
            transportRuntimeNote = uiState.transportRuntimeNote,
            onTransportPreferenceSelected = viewModel::setTransportPreference,
        )

        AppearanceCard(
            themePreference = uiState.themePreference,
            showExtensionStatusStrip = uiState.showExtensionStatusStrip,
            onThemePreferenceSelected = viewModel::setThemePreference,
            onToggleExtensionStatusStrip = viewModel::toggleExtensionStatusStrip,
        )

        DeliveryModesCard(
            steeringMode = uiState.steeringMode,
            followUpMode = uiState.followUpMode,
            isUpdatingSteeringMode = uiState.isUpdatingSteeringMode,
            isUpdatingFollowUpMode = uiState.isUpdatingFollowUpMode,
            onSteeringModeSelected = viewModel::setSteeringMode,
            onFollowUpModeSelected = viewModel::setFollowUpMode,
        )

        AssistantCard()

        ChatHelpCard()

        AppInfoCard(
            version = uiState.appVersion,
        )
    }
}

@Composable
private fun AssistantCard() {
    val context = LocalContext.current
    PiCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Assistant",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                "Set FRESKIEL as the digital assistant to reach the composer from the assist gesture. " +
                    "The focused screen's text and one screenshot are attached when the gesture starts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PiButton(
            label = "Open assistant settings",
            onClick = {
                val roleIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        assistantRoleIntent(context)
                    } else {
                        null
                    }
                val intent =
                    roleIntent
                        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                        } else {
                            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                        }
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.padding(top = PiSpacing.sm),
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Suppress("ReturnCount")
private fun assistantRoleIntent(context: Context): Intent? {
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
    if (!roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) return null
    return roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
}

@Composable
private fun ConnectionStatusCard(
    state: SettingsUiState,
    transientStatusMessage: String?,
    onPing: () -> Unit,
) {
    PiCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Connection",
            style = MaterialTheme.typography.titleMedium,
        )

        ConnectionStatusRow(
            connectionStatus = state.connectionStatus,
            isChecking = state.isChecking,
        )

        ConnectionMessages(
            state = state,
            transientStatusMessage = transientStatusMessage,
        )

        PiButton(
            label = "Check Connection",
            onClick = onPing,
            enabled = !state.isChecking,
            modifier = Modifier.padding(top = PiSpacing.sm),
        )
    }
}

@Composable
private fun ConnectionStatusRow(
    connectionStatus: ConnectionStatus?,
    isChecking: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val statusColor =
            when (connectionStatus) {
                ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
                ConnectionStatus.CHECKING -> MaterialTheme.colorScheme.tertiary
                null -> MaterialTheme.colorScheme.outline
            }

        Text(
            text = "Status: ${connectionStatus?.name ?: "Unknown"}",
            color = statusColor,
        )

        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.padding(start = 8.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun ConnectionMessages(
    state: SettingsUiState,
    transientStatusMessage: String?,
) {
    state.piVersion?.let { version ->
        Text(
            text = "Active model: $version",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    state.sessionName?.let { sessionName ->
        Text(
            text = "Active session: $sessionName",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    state.pendingMessageCount?.let { pendingCount ->
        Text(
            text = "Queued messages: $pendingCount",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    state.clientId?.let { clientId ->
        Text(
            text = "Client ID: $clientId",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    transientStatusMessage?.let { status ->
        Text(
            text = status,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    state.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AgentAutomationCard(
    autoCompactionEnabled: Boolean,
    autoRetryEnabled: Boolean,
    onToggleAutoCompaction: () -> Unit,
    onToggleAutoRetry: () -> Unit,
) {
    PiCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Automation",
            style = MaterialTheme.typography.titleMedium,
        )

        SettingsToggleRow(
            title = "Auto-compact context",
            description = "Automatically compact conversation when nearing token limit",
            checked = autoCompactionEnabled,
            onToggle = onToggleAutoCompaction,
        )

        SettingsToggleRow(
            title = "Auto-retry on errors",
            description = "Automatically retry failed requests with exponential backoff",
            checked = autoRetryEnabled,
            onToggle = onToggleAutoRetry,
        )
    }
}

@Composable
private fun TransportCard(
    transportPreference: TransportPreference,
    effectiveTransportPreference: TransportPreference,
    transportRuntimeNote: String,
    onTransportPreferenceSelected: (TransportPreference) -> Unit,
) {
    PiCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Connection routing",
            style = MaterialTheme.typography.titleMedium,
        )

        TransportPreferenceRow(
            selectedPreference = transportPreference,
            effectivePreference = effectiveTransportPreference,
            runtimeNote = transportRuntimeNote,
            onPreferenceSelected = onTransportPreferenceSelected,
        )
    }
}

@Composable
private fun AppearanceCard(
    themePreference: ThemePreference,
    showExtensionStatusStrip: Boolean,
    onThemePreferenceSelected: (ThemePreference) -> Unit,
    onToggleExtensionStatusStrip: () -> Unit,
) {
    PiCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
        )

        ThemePreferenceRow(
            selectedPreference = themePreference,
            onPreferenceSelected = onThemePreferenceSelected,
        )

        SettingsToggleRow(
            title = "Show extension status strip",
            description = "Show compact extension runtime updates in chat",
            checked = showExtensionStatusStrip,
            onToggle = onToggleExtensionStatusStrip,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun DeliveryModesCard(
    steeringMode: String,
    followUpMode: String,
    isUpdatingSteeringMode: Boolean,
    isUpdatingFollowUpMode: Boolean,
    onSteeringModeSelected: (String) -> Unit,
    onFollowUpModeSelected: (String) -> Unit,
) {
    PiCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Streaming delivery",
            style = MaterialTheme.typography.titleMedium,
        )

        ModeSelectorRow(
            title = "Steering mode",
            description = "How steer messages are delivered while streaming",
            selectedMode = steeringMode,
            isUpdating = isUpdatingSteeringMode,
            onModeSelected = onSteeringModeSelected,
        )

        ModeSelectorRow(
            title = "Follow-up mode",
            description = "How follow-up messages are queued while streaming",
            selectedMode = followUpMode,
            isUpdating = isUpdatingFollowUpMode,
            onModeSelected = onFollowUpModeSelected,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun TransportPreferenceRow(
    selectedPreference: TransportPreference,
    effectivePreference: TransportPreference,
    runtimeNote: String,
    onPreferenceSelected: (TransportPreference) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Transport preference",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Preferred transport between the app and bridge runtime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportOptionButton(
                label = "Auto",
                selected = selectedPreference == TransportPreference.AUTO,
                onClick = { onPreferenceSelected(TransportPreference.AUTO) },
            )
            TransportOptionButton(
                label = "WebSocket",
                selected = selectedPreference == TransportPreference.WEBSOCKET,
                onClick = { onPreferenceSelected(TransportPreference.WEBSOCKET) },
            )
            TransportOptionButton(
                label = "SSE",
                selected = selectedPreference == TransportPreference.SSE,
                onClick = { onPreferenceSelected(TransportPreference.SSE) },
            )
        }

        Text(
            text = "Effective: ${effectivePreference.value}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        if (runtimeNote.isNotBlank()) {
            Text(
                text = runtimeNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemePreferenceRow(
    selectedPreference: ThemePreference,
    onPreferenceSelected: (ThemePreference) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Choose app appearance mode",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeOptionButton(
                label = "System",
                selected = selectedPreference == ThemePreference.SYSTEM,
                onClick = { onPreferenceSelected(ThemePreference.SYSTEM) },
            )
            ThemeOptionButton(
                label = "Light",
                selected = selectedPreference == ThemePreference.LIGHT,
                onClick = { onPreferenceSelected(ThemePreference.LIGHT) },
            )
            ThemeOptionButton(
                label = "Dark",
                selected = selectedPreference == ThemePreference.DARK,
                onClick = { onPreferenceSelected(ThemePreference.DARK) },
            )
        }
    }
}

@Composable
private fun ModeSelectorRow(
    title: String,
    description: String,
    selectedMode: String,
    isUpdating: Boolean,
    onModeSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeOptionButton(
                label = "All",
                selected = selectedMode == SettingsViewModel.MODE_ALL,
                enabled = !isUpdating,
                onClick = { onModeSelected(SettingsViewModel.MODE_ALL) },
            )
            ModeOptionButton(
                label = "One at a time",
                selected = selectedMode == SettingsViewModel.MODE_ONE_AT_A_TIME,
                enabled = !isUpdating,
                onClick = { onModeSelected(SettingsViewModel.MODE_ONE_AT_A_TIME) },
            )
        }
    }
}

@Composable
private fun TransportOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PiButton(
        label = label,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun ThemeOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PiButton(
        label = label,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun ModeOptionButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PiButton(
        label = label,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun ChatHelpCard() {
    PiCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Chat actions & gestures",
            style = MaterialTheme.typography.titleMedium,
        )

        HelpItem(
            action = "Send",
            help = "Tap the send icon or use keyboard Send action",
        )
        HelpItem(
            action = "Commands",
            help = "Tap the menu icon in the prompt field to open slash commands",
        )
        HelpItem(
            action = "Model",
            help = "Tap model chip to cycle; long-press to open full picker",
        )
        HelpItem(
            action = "Thinking/Tool output",
            help = "Tap show more/show less to expand or collapse long sections",
        )
        HelpItem(
            action = "Tree",
            help = "Open Tree from chat header to inspect branches and fork from entries",
        )
        HelpItem(
            action = "Bash & Stats",
            help = "Use terminal and chart icons in chat header",
        )
    }
}

@Composable
private fun HelpItem(
    action: String,
    help: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = action,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppInfoCard(version: String) {
    PiCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Version: $version",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private const val STATUS_MESSAGE_DURATION_MS = 3_000L
