package com.example.ui.settings

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserSettings
import com.example.data.remote.AiCatalog
import com.example.data.remote.AiProviderType
import com.example.data.remote.ConnectionStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppThemeMode
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  settings: UserSettings,
  connectionStatus: ConnectionStatus,
  snackbarHostState: SnackbarHostState,
  onSelectLanguage: (AppLanguage) -> Unit,
  onSelectTheme: (AppThemeMode) -> Unit,
  onSelectProviderAndModel: (String, String) -> Unit,
  onTestConnection: () -> Unit,
  onSetMemoryEnabled: (Boolean) -> Unit,
  onSaveMemoryNotes: (String) -> Unit,
  onClearMemory: () -> Unit,
  onClearAllConversations: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val isAr = settings.language == AppLanguage.AR
  val scope = rememberCoroutineScope()

  var memoryDraft by remember(settings.memoryNotes) { mutableStateOf(settings.memoryNotes) }
  var showClearHistoryDialog by remember { mutableStateOf(false) }

  LaunchedEffect(settings.memoryNotes) {
    memoryDraft = settings.memoryNotes
  }

  val selectedProvider = AiProviderType.fromId(settings.providerId)
  val providerModels = AiCatalog.modelsForProvider(selectedProvider)

  if (showClearHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showClearHistoryDialog = false },
      title = { Text(strings.settingsClearHistoryConfirmTitle) },
      text = { Text(strings.settingsClearHistoryConfirmBody) },
      confirmButton = {
        Button(
          onClick = {
            onClearAllConversations()
            showClearHistoryDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_clear_history_button"),
        ) {
          Text(strings.settingsConfirmDelete)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearHistoryDialog = false }) {
          Text(strings.settingsCancel)
        }
      },
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize()
        .navigationBarsPadding(),
    ) {
      item {
        Text(
          text = strings.settingsTitle,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = strings.settingsSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // 1. Language & RTL/LTR Section
      item {
        SettingsCard(title = strings.settingsLanguageSection, icon = Icons.Filled.Language) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            FilterChip(
              selected = settings.language == AppLanguage.AR,
              onClick = { onSelectLanguage(AppLanguage.AR) },
              label = { Text(strings.settingsLanguageArabic) },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("lang_ar_button"),
            )
            FilterChip(
              selected = settings.language == AppLanguage.EN,
              onClick = { onSelectLanguage(AppLanguage.EN) },
              label = { Text(strings.settingsLanguageEnglish) },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("lang_en_button"),
            )
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = strings.settingsRtlActiveNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // 2. Theme Section
      item {
        SettingsCard(title = strings.settingsThemeSection, icon = Icons.Filled.Palette) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ThemeOptionRow(
              label = strings.settingsThemeDark,
              selected = settings.themeMode == AppThemeMode.DARK,
              tag = "theme_dark_button",
              onClick = { onSelectTheme(AppThemeMode.DARK) },
            )
            ThemeOptionRow(
              label = strings.settingsThemeLight,
              selected = settings.themeMode == AppThemeMode.LIGHT,
              tag = "theme_light_button",
              onClick = { onSelectTheme(AppThemeMode.LIGHT) },
            )
            ThemeOptionRow(
              label = strings.settingsThemeSystem,
              selected = settings.themeMode == AppThemeMode.SYSTEM,
              tag = "theme_system_button",
              onClick = { onSelectTheme(AppThemeMode.SYSTEM) },
            )
          }
        }
      }

      // 3. AI Provider, Model & Real Connection Status
      item {
        SettingsCard(title = strings.settingsAiProviderSection, icon = Icons.Filled.SmartToy) {
          Text(
            text = strings.settingsSelectProvider,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.height(8.dp))
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AiProviderType.entries.forEach { provider ->
              val isSelected = provider == selectedProvider
              val configured = provider.isConfigured()
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(MaterialTheme.shapes.medium)
                  .clickable {
                    val defaultModel = AiCatalog.modelsForProvider(provider).first().id
                    onSelectProviderAndModel(provider.id, defaultModel)
                  }
                  .testTag("provider_${provider.id}"),
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = if (isAr) provider.displayNameAr else provider.displayNameEn,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = if (configured) strings.settingsConfiguredTag else strings.settingsNotConfiguredTag,
                      style = MaterialTheme.typography.labelSmall,
                      color = if (configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                  }
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = if (isAr) provider.rateLimitSummaryAr else provider.rateLimitSummaryEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(14.dp))
          Text(
            text = strings.settingsSelectModel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.height(8.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            providerModels.forEach { model ->
              val selected = model.id == settings.modelId
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(MaterialTheme.shapes.small)
                  .clickable { onSelectProviderAndModel(selectedProvider.id, model.id) }
                  .padding(vertical = 4.dp)
                  .testTag("model_${model.id}"),
              ) {
                RadioButton(
                  selected = selected,
                  onClick = { onSelectProviderAndModel(selectedProvider.id, model.id) },
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isAr) model.nameAr else model.nameEn,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                  Text(
                    text = "${model.id} • ${if (isAr) model.badgeAr else model.badgeEn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
          Spacer(Modifier.height(12.dp))

          // Real Connection Status Box
          Text(
            text = strings.settingsConnectionStatus,
            style = MaterialTheme.typography.labelLarge,
          )
          Spacer(Modifier.height(6.dp))
          ConnectionStatusBanner(connectionStatus = connectionStatus)
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = onTestConnection,
            enabled = connectionStatus !is ConnectionStatus.Checking,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("test_connection_button"),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.settingsTestConnectionBtn)
          }
        }
      }

      // 4. Transparent Free-Tier Rate Limits
      item {
        SettingsCard(title = strings.settingsRateLimitsTitle, icon = Icons.Filled.Info) {
          Text(
            text = strings.settingsRateLimitsBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // 5. Conversation History & Optional Memory
      item {
        SettingsCard(title = strings.settingsHistoryMemoryTitle, icon = Icons.Filled.Memory) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text(
                text = strings.settingsMemoryToggleTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = strings.settingsMemoryToggleDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = settings.memoryEnabled,
              onCheckedChange = onSetMemoryEnabled,
              modifier = Modifier.testTag("memory_toggle_switch"),
            )
          }

          if (settings.memoryEnabled) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
              value = memoryDraft,
              onValueChange = { memoryDraft = it },
              placeholder = { Text(strings.settingsMemoryPlaceholder) },
              minLines = 3,
              maxLines = 5,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("memory_notes_input"),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                  onSaveMemoryNotes(memoryDraft)
                  scope.launch { snackbarHostState.showSnackbar(strings.builderProjectSaved) }
                },
                modifier = Modifier.testTag("save_memory_button"),
              ) {
                Text(strings.settingsSaveMemoryBtn)
              }
              OutlinedButton(
                onClick = {
                  memoryDraft = ""
                  onClearMemory()
                },
                modifier = Modifier.testTag("clear_memory_button"),
              ) {
                Text(strings.settingsClearMemoryBtn)
              }
            }
          }

          Spacer(Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
          Spacer(Modifier.height(12.dp))

          OutlinedButton(
            onClick = { showClearHistoryDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("clear_history_button"),
          ) {
            Text(strings.settingsClearHistoryBtn)
          }
        }
      }

      // 6. Server / Secrets Panel Configuration Instructions & Security Notice
      item {
        SettingsCard(title = strings.settingsServerSetupTitle, icon = Icons.Filled.Security) {
          Text(
            text = strings.settingsServerSetupBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.height(12.dp))
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = strings.settingsSecurityNoticeTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = strings.settingsSecurityNoticeBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsCard(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Spacer(Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun ThemeOptionRow(
  label: String,
  selected: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.small)
      .clickable(onClick = onClick)
      .testTag(tag),
  ) {
    RadioButton(selected = selected, onClick = onClick)
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun ConnectionStatusBanner(connectionStatus: ConnectionStatus) {
  val strings = LocalAppStrings.current
  val (statusText, isOk, isBusy) = when (connectionStatus) {
    ConnectionStatus.Unverified -> Triple(strings.settingsStatusUnverified, false, false)
    ConnectionStatus.Checking -> Triple(strings.settingsStatusChecking, false, true)
    is ConnectionStatus.Connected -> Triple(
      "${strings.settingsStatusConnected} (${connectionStatus.modelId} • ${connectionStatus.latencyMs} ms)",
      true,
      false,
    )
    is ConnectionStatus.MissingKey -> Triple(strings.settingsStatusMissingKey, false, false)
    is ConnectionStatus.RateLimited -> Triple(strings.settingsStatusRateLimited, false, false)
    is ConnectionStatus.Failed -> Triple("${strings.settingsStatusError}: ${connectionStatus.reason}", false, false)
  }

  Surface(
    shape = MaterialTheme.shapes.small,
    color = if (isOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("connection_status_banner"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(12.dp),
    ) {
      when {
        isBusy -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        isOk -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        else -> Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
      }
      Spacer(Modifier.width(10.dp))
      Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
