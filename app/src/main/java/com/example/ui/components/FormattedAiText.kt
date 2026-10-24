package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFontFamily

private sealed interface MarkdownSegment {
  data class Prose(val text: String) : MarkdownSegment
  data class CodeBlock(val language: String, val code: String) : MarkdownSegment
  data class TableBlock(val rows: List<List<String>>) : MarkdownSegment
}

@Composable
fun FormattedAiText(
  markdown: String,
  onCopied: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val segments = remember(markdown) { parseMarkdownSegments(markdown) }
  val clipboard = LocalClipboardManager.current
  val strings = LocalAppStrings.current

  SelectionContainer(modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      segments.forEach { segment ->
        when (segment) {
          is MarkdownSegment.CodeBlock -> {
            // Code blocks MUST stay Left-to-Right (LTR) even when interface is Arabic RTL
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                  .fillMaxWidth()
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small,
                  ),
              ) {
                Column {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = segment.language.ifBlank { "code" }.uppercase(),
                      style = MaterialTheme.typography.labelMedium,
                      fontFamily = JetBrainsMonoFontFamily,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(
                      onClick = {
                        clipboard.setText(AnnotatedString(segment.code))
                        onCopied()
                      }
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = strings.copyCode,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(Modifier.width(6.dp))
                      Text(
                        text = strings.copyCode,
                        style = MaterialTheme.typography.labelMedium,
                      )
                    }
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState())
                      .padding(12.dp)
                  ) {
                    Text(
                      text = segment.code,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = 13.sp,
                      lineHeight = 20.sp,
                      color = MaterialTheme.colorScheme.onBackground,
                    )
                  }
                }
              }
            }
          }

          is MarkdownSegment.TableBlock -> {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                .horizontalScroll(rememberScrollState())
            ) {
              segment.rows.forEachIndexed { rowIdx, cells ->
                val isHeader = rowIdx == 0
                Row(
                  modifier = Modifier
                    .background(
                      if (isHeader) MaterialTheme.colorScheme.surfaceVariant
                      else MaterialTheme.colorScheme.surface
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  cells.forEach { cell ->
                    Text(
                      text = cell,
                      style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.width(140.dp),
                    )
                  }
                }
                if (rowIdx < segment.rows.lastIndex) {
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
              }
            }
          }

          is MarkdownSegment.Prose -> {
            ProseParagraphs(segment.text)
          }
        }
      }
    }
  }
}

@Composable
private fun ProseParagraphs(text: String) {
  val lines = remember(text) { text.lines() }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    lines.forEach { rawLine ->
      val line = rawLine.trimEnd()
      when {
        line.isBlank() -> Spacer(Modifier.height(2.dp))
        line.startsWith("### ") -> Text(
          text = buildInlineStyled(line.removePrefix("### ")),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        line.startsWith("## ") -> Text(
          text = buildInlineStyled(line.removePrefix("## ")),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        line.startsWith("# ") -> Text(
          text = buildInlineStyled(line.removePrefix("# ")),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        line.startsWith("- ") || line.startsWith("* ") -> {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "•",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = buildInlineStyled(line.drop(2)),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
        else -> Text(
          text = buildInlineStyled(line),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun buildInlineStyled(raw: String): AnnotatedString {
  val codeBg = MaterialTheme.colorScheme.surfaceVariant
  val codeColor = MaterialTheme.colorScheme.primary
  return buildAnnotatedString {
    var i = 0
    while (i < raw.length) {
      when {
        raw.startsWith("**", i) -> {
          val end = raw.indexOf("**", i + 2)
          if (end != -1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
              append(raw.substring(i + 2, end))
            }
            i = end + 2
          } else {
            append(raw[i])
            i++
          }
        }
        raw[i] == '`' -> {
          val end = raw.indexOf('`', i + 1)
          if (end != -1) {
            withStyle(
              SpanStyle(
                fontFamily = JetBrainsMonoFontFamily,
                background = codeBg,
                color = codeColor,
              )
            ) {
              append(" ${raw.substring(i + 1, end)} ")
            }
            i = end + 1
          } else {
            append(raw[i])
            i++
          }
        }
        else -> {
          append(raw[i])
          i++
        }
      }
    }
  }
}

private fun parseMarkdownSegments(markdown: String): List<MarkdownSegment> {
  val result = mutableListOf<MarkdownSegment>()
  val lines = markdown.lines()
  var idx = 0
  val proseBuffer = mutableListOf<String>()

  fun flushProse() {
    if (proseBuffer.isNotEmpty()) {
      val joined = proseBuffer.joinToString("\n").trim()
      if (joined.isNotEmpty()) result += MarkdownSegment.Prose(joined)
      proseBuffer.clear()
    }
  }

  while (idx < lines.size) {
    val line = lines[idx]
    val trimmed = line.trim()
    if (trimmed.startsWith("```")) {
      flushProse()
      val lang = trimmed.removePrefix("```").trim()
      idx++
      val codeLines = mutableListOf<String>()
      while (idx < lines.size && !lines[idx].trim().startsWith("```")) {
        codeLines += lines[idx]
        idx++
      }
      result += MarkdownSegment.CodeBlock(lang, codeLines.joinToString("\n"))
      idx++ // skip closing ```
    } else if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
      flushProse()
      val tableRows = mutableListOf<List<String>>()
      while (idx < lines.size) {
        val rowLine = lines[idx].trim()
        if (!rowLine.startsWith("|")) break
        val cells = rowLine.trim('|').split("|").map { it.trim() }
        val isSeparator = cells.all { cell -> cell.all { ch -> ch == '-' || ch == ':' || ch == ' ' } }
        if (!isSeparator && cells.isNotEmpty()) {
          tableRows += cells
        }
        idx++
      }
      if (tableRows.isNotEmpty()) {
        result += MarkdownSegment.TableBlock(tableRows)
      }
    } else {
      proseBuffer += line
      idx++
    }
  }
  flushProse()
  return result
}
