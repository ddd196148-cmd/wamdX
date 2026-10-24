package com.example.ui.tools

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AiToolId
import com.example.ui.ToolUiState
import com.example.ui.components.FormattedAiText
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch

private data class ToolCardMeta(
  val id: AiToolId?,
  val title: String,
  val desc: String,
  val hint: String,
  val icon: ImageVector,
  val opensAppBuilder: Boolean = false,
)

@Composable
fun ToolsScreen(
  toolState: ToolUiState,
  snackbarHostState: SnackbarHostState,
  onSelectTool: (AiToolId?) -> Unit,
  onOpenBuilder: () -> Unit,
  onUpdateInput: (String) -> Unit,
  onUpdateTargetLanguage: (String) -> Unit,
  onAttachFile: (android.content.Context, android.net.Uri) -> Unit,
  onRunTool: () -> Unit,
  onContinueInChat: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val isAr = LocalAppLanguage.current == AppLanguage.AR

  val cards = listOf(
    ToolCardMeta(AiToolId.WRITING, strings.toolWritingTitle, strings.toolWritingDesc, strings.toolWritingHint, Icons.Filled.EditNote),
    ToolCardMeta(AiToolId.TRANSLATE, strings.toolTranslateTitle, strings.toolTranslateDesc, strings.toolTranslateHint, Icons.Filled.Translate),
    ToolCardMeta(AiToolId.SUMMARIZE, strings.toolSummarizeTitle, strings.toolSummarizeDesc, strings.toolSummarizeHint, Icons.Filled.Summarize),
    ToolCardMeta(AiToolId.IMPROVE, strings.toolImproveTitle, strings.toolImproveDesc, strings.toolImproveHint, Icons.Filled.AutoFixHigh),
    ToolCardMeta(AiToolId.CODE, strings.toolCodeTitle, strings.toolCodeDesc, strings.toolCodeHint, Icons.Filled.Code),
    ToolCardMeta(AiToolId.MATH_SCIENCE, strings.toolMathScienceTitle, strings.toolMathScienceDesc, strings.toolMathScienceHint, Icons.Filled.Calculate),
    ToolCardMeta(AiToolId.IDEAS, strings.toolIdeasTitle, strings.toolIdeasDesc, strings.toolIdeasHint, Icons.Filled.Lightbulb),
    ToolCardMeta(AiToolId.FILE_ANALYSIS, strings.toolFileAnalysisTitle, strings.toolFileAnalysisDesc, strings.toolFileAnalysisHint, Icons.Filled.FindInPage),
    ToolCardMeta(null, strings.toolBuilderTitle, strings.toolBuilderDesc, "", Icons.Filled.Web, opensAppBuilder = true),
  )

  val activeMeta = cards.firstOrNull { it.id != null && it.id == toolState.activeTool }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    if (activeMeta == null) {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .widthIn(max = 860.dp)
          .fillMaxSize()
          .navigationBarsPadding(),
      ) {
        item {
          Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
              text = strings.toolsTitle,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = strings.toolsSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        items(cards.chunked(2)) { rowItems ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            rowItems.forEach { card ->
              Surface(
                shape = MaterialTheme.shapes.large,
                color = if (card.opensAppBuilder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                  .weight(1f)
                  .clip(MaterialTheme.shapes.large)
                  .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                  .clickable {
                    if (card.opensAppBuilder) onOpenBuilder() else onSelectTool(card.id)
                  }
                  .testTag("tool_card_${card.id?.name ?: "BUILDER"}"),
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(44.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = card.icon,
                        contentDescription = card.title,
                        tint = MaterialTheme.colorScheme.secondary,
                      )
                    }
                  }
                  Spacer(Modifier.height(12.dp))
                  Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = card.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            if (rowItems.size == 1) {
              Spacer(Modifier.weight(1f))
            }
          }
        }
      }
    } else {
      BackHandler { onSelectTool(null) }
      ActiveToolRunnerView(
        meta = activeMeta,
        toolState = toolState,
        isAr = isAr,
        snackbarHostState = snackbarHostState,
        onBack = { onSelectTool(null) },
        onUpdateInput = onUpdateInput,
        onUpdateTargetLanguage = onUpdateTargetLanguage,
        onAttachFile = onAttachFile,
        onRunTool = onRunTool,
        onContinueInChat = onContinueInChat,
      )
    }
  }
}

@Composable
private fun ActiveToolRunnerView(
  meta: ToolCardMeta,
  toolState: ToolUiState,
  isAr: Boolean,
  snackbarHostState: SnackbarHostState,
  onBack: () -> Unit,
  onUpdateInput: (String) -> Unit,
  onUpdateTargetLanguage: (String) -> Unit,
  onAttachFile: (android.content.Context, android.net.Uri) -> Unit,
  onRunTool: () -> Unit,
  onContinueInChat: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()

  val docPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) onAttachFile(context, uri)
  }

  val targetLangOptions = listOf(
    "العربية (Arabic)",
    "English",
    "Français (French)",
    "Español (Spanish)",
    "Türkçe (Turkish)",
    "Deutsch (German)",
  )

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier
      .widthIn(max = 840.dp)
      .fillMaxSize()
      .navigationBarsPadding(),
  ) {
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
          onClick = onBack,
          modifier = Modifier.testTag("back_to_tools_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = strings.backToTools,
          )
          Spacer(Modifier.width(6.dp))
          Text(strings.backToTools)
        }
      }
      Text(
        text = meta.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = meta.desc,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (meta.id == AiToolId.TRANSLATE) {
      item {
        Text(
          text = strings.targetLanguageLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
          targetLangOptions.forEach { langOption ->
            FilterChip(
              selected = toolState.targetLanguage == langOption,
              onClick = { onUpdateTargetLanguage(langOption) },
              label = { Text(langOption) },
            )
          }
        }
      }
    }

    if (meta.id == AiToolId.FILE_ANALYSIS) {
      item {
        OutlinedButton(
          onClick = {
            docPicker.launch(
              arrayOf("text/*", "image/*", "application/pdf", "application/json", "application/xml")
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("tool_pick_file_button"),
        ) {
          Icon(Icons.Filled.AttachFile, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            toolState.attachment?.name ?: strings.selectFileForAnalysis
          )
        }
      }
    }

    item {
      OutlinedTextField(
        value = toolState.inputText,
        onValueChange = onUpdateInput,
        placeholder = { Text(meta.hint) },
        minLines = 5,
        maxLines = 10,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tool_input_field"),
      )
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = onRunTool,
        enabled = !toolState.isRunning &&
          (toolState.inputText.isNotBlank() || toolState.attachment != null),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("run_tool_button"),
      ) {
        if (toolState.isRunning) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(Modifier.width(10.dp))
          Text(strings.aiThinking)
        } else {
          Text(strings.runToolButton)
        }
      }
    }

    val errorMsg = if (isAr) toolState.errorAr else toolState.errorEn
    if (errorMsg != null) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMsg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(14.dp),
          )
        }
      }
    }

    if (toolState.resultText != null) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = MaterialTheme.shapes.large,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = strings.toolResultHeading,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                  onClick = {
                    clipboard.setText(AnnotatedString(toolState.resultText))
                    scope.launch { snackbarHostState.showSnackbar(strings.copiedToast) }
                  }
                ) {
                  Icon(Icons.Outlined.ContentCopy, contentDescription = strings.copyResponse, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(strings.copyResponse)
                }
                OutlinedButton(
                  onClick = onContinueInChat,
                  modifier = Modifier.testTag("continue_in_chat_button"),
                ) {
                  Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(strings.continueInChatButton)
                }
              }
            }
            Spacer(Modifier.height(10.dp))
            FormattedAiText(
              markdown = toolState.resultText,
              onCopied = { scope.launch { snackbarHostState.showSnackbar(strings.copiedToast) } },
            )
          }
        }
      }
    }
  }
}
