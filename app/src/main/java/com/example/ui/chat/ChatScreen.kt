package com.example.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MessageEntity
import com.example.data.local.UserSettings
import com.example.data.remote.AiCatalog
import com.example.data.remote.AiProviderType
import com.example.ui.ChatUiState
import com.example.ui.components.FormattedAiText
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
  messages: List<MessageEntity>,
  chatState: ChatUiState,
  settings: UserSettings,
  snackbarHostState: SnackbarHostState,
  onInputChanged: (String) -> Unit,
  onSendMessage: (String?) -> Unit,
  onAttachUri: (android.content.Context, android.net.Uri) -> Unit,
  onClearAttachment: () -> Unit,
  onSwitchProvider: (AiProviderType) -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  val filePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      onAttachUri(context, uri)
    }
  }

  LaunchedEffect(messages.size, chatState.isSending) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val activeModelSpec = AiCatalog.findModel(settings.modelId)
  val isAr = settings.language == AppLanguage.AR

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Central Chat Area
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (messages.isEmpty() && !chatState.isSending) {
        WelcomeChatEmptyState(
          modelLabel = if (isAr) activeModelSpec.nameAr else activeModelSpec.nameEn,
          onPromptClicked = { prompt -> onSendMessage(prompt) },
        )
      } else {
        LazyColumn(
          state = listState,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxSize()
            .testTag("chat_messages_list"),
        ) {
          items(messages, key = { it.id }) { msg ->
            ChatMessageCard(
              message = msg,
              onCopyAll = {
                clipboard.setText(AnnotatedString(msg.content))
                scope.launch { snackbarHostState.showSnackbar(strings.copiedToast) }
              },
              onCodeCopied = {
                scope.launch { snackbarHostState.showSnackbar(strings.copiedToast) }
              },
            )
          }

          if (chatState.isSending) {
            item(key = "thinking_indicator") {
              Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = strings.aiThinking,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Error / Rate-limit Banner
    val errorText = if (isAr) chatState.errorAr else chatState.errorEn
    AnimatedVisibility(visible = errorText != null) {
      if (errorText != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("chat_error_banner"),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
              )
              Text(
                text = errorText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (chatState.lastFailedPrompt != null) {
                OutlinedButton(
                  onClick = { onSendMessage(chatState.lastFailedPrompt) },
                  modifier = Modifier.testTag("retry_message_button"),
                ) {
                  Text(strings.retryAction)
                }
              }
              if (chatState.fallbackProvider != null) {
                Button(
                  onClick = { onSwitchProvider(chatState.fallbackProvider) },
                ) {
                  Text(
                    if (isAr) chatState.fallbackProvider.displayNameAr
                    else chatState.fallbackProvider.displayNameEn
                  )
                }
              } else {
                TextButton(onClick = onOpenSettings) {
                  Text(strings.openSettingsAction)
                }
              }
            }
          }
        }
      }
    }

    // Bottom Message Composer
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding(),
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 840.dp)
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
      ) {
        if (chatState.pendingAttachment != null) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(bottom = 8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "${strings.attachedLabel}: ${chatState.pendingAttachment.name}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Spacer(Modifier.width(6.dp))
              IconButton(
                onClick = onClearAttachment,
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = strings.removeAttachment,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }

        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          IconButton(
            onClick = {
              filePicker.launch(
                arrayOf("text/*", "image/*", "application/pdf", "application/json", "application/xml")
              )
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("attach_file_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.AttachFile,
              contentDescription = strings.attachFileTooltip,
              tint = MaterialTheme.colorScheme.primary,
            )
          }

          OutlinedTextField(
            value = chatState.inputText,
            onValueChange = onInputChanged,
            placeholder = {
              Text(
                text = strings.messagePlaceholder,
                style = MaterialTheme.typography.bodyMedium,
              )
            },
            maxLines = 5,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("chat_input_field"),
          )

          val canSend = !chatState.isSending &&
            (chatState.inputText.isNotBlank() || chatState.pendingAttachment != null)

          FilledIconButton(
            onClick = { onSendMessage(null) },
            enabled = canSend,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
              .size(48.dp)
              .testTag("send_message_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = strings.sendTooltip,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WelcomeChatEmptyState(
  modelLabel: String,
  onPromptClicked: (String) -> Unit,
) {
  val strings = LocalAppStrings.current

  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier
      .widthIn(max = 840.dp)
      .fillMaxSize(),
  ) {
    item {
      Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge),
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Image(
            painter = painterResource(id = R.drawable.img_wamdx_hero),
            contentDescription = stringResource(R.string.cd_hero_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(176.dp),
          )
          Box(
            modifier = Modifier
              .matchParentSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
          ) {
            AssistChip(
              onClick = {},
              label = { Text(strings.freeAccessBadge) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
              },
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = strings.welcomeHeadline,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
              text = strings.welcomeSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = modelLabel,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
    }

    items(strings.starterPrompts.chunked(2)) { pair ->
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        pair.forEachIndexed { idx, item ->
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .clip(MaterialTheme.shapes.medium)
              .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
              .clickable { onPromptClicked(item.prompt) }
              .testTag("starter_prompt_${item.category}_$idx"),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = item.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = item.prompt,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        if (pair.size == 1) {
          Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun ChatMessageCard(
  message: MessageEntity,
  onCopyAll: () -> Unit,
  onCodeCopied: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val isUser = message.role == "user"

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = if (isUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .widthIn(max = 720.dp)
        .border(
          width = 1.dp,
          color = if (isUser) MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline,
          shape = MaterialTheme.shapes.large,
        ),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        if (message.attachmentName != null) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(bottom = 8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = message.attachmentName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

        if (isUser) {
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        } else {
          FormattedAiText(
            markdown = message.content,
            onCopied = onCodeCopied,
          )
          Spacer(Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = message.modelUsed ?: "WamdX AI",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
              onClick = onCopyAll,
              modifier = Modifier.testTag("copy_response_${message.id}"),
            ) {
              Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = strings.copyResponse,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = strings.copyResponse,
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
      }
    }
  }
}
