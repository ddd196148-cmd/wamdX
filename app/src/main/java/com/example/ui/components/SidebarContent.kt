package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ConversationEntity
import com.example.ui.WorkspaceSection
import com.example.ui.i18n.LocalAppStrings

@Composable
fun SidebarContent(
  currentSection: WorkspaceSection,
  conversations: List<ConversationEntity>,
  activeConversationId: Long?,
  onNewChat: () -> Unit,
  onSelectSection: (WorkspaceSection) -> Unit,
  onSelectConversation: (Long) -> Unit,
  onDeleteConversation: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current

  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding()
      .padding(horizontal = 14.dp, vertical = 12.dp),
  ) {
    // Brand Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
              )
            )
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(Modifier.width(12.dp))
      Column {
        Text(
          text = strings.appName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = strings.freeAccessBadge,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    Spacer(Modifier.height(12.dp))

    // Primary New Chat CTA
    Button(
      onClick = onNewChat,
      shape = MaterialTheme.shapes.medium,
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("new_chat_button"),
    ) {
      Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = strings.navNewChat,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = strings.navNewChat,
        style = MaterialTheme.typography.labelLarge,
      )
    }

    Spacer(Modifier.height(14.dp))

    // Main Sections
    NavigationDrawerItem(
      label = { Text(strings.navChat) },
      selected = currentSection == WorkspaceSection.CHAT,
      onClick = { onSelectSection(WorkspaceSection.CHAT) },
      icon = {
        Icon(
          imageVector = if (currentSection == WorkspaceSection.CHAT) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
          contentDescription = strings.navChat,
        )
      },
      colors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
      modifier = Modifier.testTag("nav_chat"),
    )

    NavigationDrawerItem(
      label = { Text(strings.navAiTools) },
      selected = currentSection == WorkspaceSection.TOOLS,
      onClick = { onSelectSection(WorkspaceSection.TOOLS) },
      icon = {
        Icon(
          imageVector = if (currentSection == WorkspaceSection.TOOLS) Icons.Filled.Build else Icons.Outlined.Handyman,
          contentDescription = strings.navAiTools,
        )
      },
      colors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.secondary,
        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
      ),
      modifier = Modifier.testTag("nav_tools"),
    )

    NavigationDrawerItem(
      label = { Text(strings.navAppBuilder) },
      selected = currentSection == WorkspaceSection.BUILDER,
      onClick = { onSelectSection(WorkspaceSection.BUILDER) },
      icon = {
        Icon(
          imageVector = Icons.Outlined.Web,
          contentDescription = strings.navAppBuilder,
        )
      },
      colors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
      modifier = Modifier.testTag("nav_builder"),
    )

    NavigationDrawerItem(
      label = { Text(strings.navSettings) },
      selected = currentSection == WorkspaceSection.SETTINGS,
      onClick = { onSelectSection(WorkspaceSection.SETTINGS) },
      icon = {
        Icon(
          imageVector = if (currentSection == WorkspaceSection.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
          contentDescription = strings.navSettings,
        )
      },
      modifier = Modifier.testTag("nav_settings"),
    )

    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    Spacer(Modifier.height(10.dp))

    // Chat History Section
    Text(
      text = strings.navChatHistory,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )

    if (conversations.isEmpty()) {
      Text(
        text = strings.noConversationsYet,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        items(conversations, key = { it.id }) { conv ->
          val isSelected = activeConversationId == conv.id && currentSection == WorkspaceSection.CHAT
          Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .clickable { onSelectConversation(conv.id) }
              .testTag("conversation_item_${conv.id}"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = conv.title.ifBlank { strings.untitledConversation },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = { onDeleteConversation(conv.id) },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("delete_conversation_${conv.id}"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.DeleteOutline,
                  contentDescription = strings.deleteConversation,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
