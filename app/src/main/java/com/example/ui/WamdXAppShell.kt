package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.builder.BuilderScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.components.SidebarContent
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.WamdXTheme
import com.example.ui.tools.ToolsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WamdXAppShell(viewModel: WamdXViewModel) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
  val conversations by viewModel.conversations.collectAsStateWithLifecycle()
  val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
  val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
  val chatState by viewModel.chatState.collectAsStateWithLifecycle()
  val toolState by viewModel.toolState.collectAsStateWithLifecycle()
  val builderState by viewModel.builderState.collectAsStateWithLifecycle()
  val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
  val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  BackHandler(enabled = currentSection != WorkspaceSection.CHAT) {
    viewModel.navigateTo(WorkspaceSection.CHAT)
  }

  WamdXTheme(
    language = settings.language,
    themeMode = settings.themeMode,
  ) {
    val strings = LocalAppStrings.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isExpanded = maxWidth >= 840.dp

      if (isExpanded) {
        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            SidebarContent(
              currentSection = currentSection,
              conversations = conversations,
              activeConversationId = activeConvId,
              onNewChat = { viewModel.startNewChat() },
              onSelectSection = { viewModel.navigateTo(it) },
              onSelectConversation = { viewModel.openConversation(it) },
              onDeleteConversation = { viewModel.deleteConversation(it) },
              modifier = Modifier.width(292.dp),
            )
            VerticalDivider(
              modifier = Modifier.fillMaxHeight(),
              color = MaterialTheme.colorScheme.outline,
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
              ActiveWorkspacePane(
                currentSection = currentSection,
                viewModel = viewModel,
                settings = settings,
                messages = messages,
                chatState = chatState,
                toolState = toolState,
                builderState = builderState,
                savedProjects = savedProjects,
                connectionStatus = connectionStatus,
                snackbarHostState = snackbarHostState,
              )
            }
          }
        }
      } else {
        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            ModalDrawerSheet(
              drawerContainerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.width(300.dp),
            ) {
              SidebarContent(
                currentSection = currentSection,
                conversations = conversations,
                activeConversationId = activeConvId,
                onNewChat = {
                  viewModel.startNewChat()
                  scope.launch { drawerState.close() }
                },
                onSelectSection = { section ->
                  viewModel.navigateTo(section)
                  scope.launch { drawerState.close() }
                },
                onSelectConversation = { id ->
                  viewModel.openConversation(id)
                  scope.launch { drawerState.close() }
                },
                onDeleteConversation = { id ->
                  viewModel.deleteConversation(id)
                },
              )
            }
          },
        ) {
          val sectionTitle = when (currentSection) {
            WorkspaceSection.CHAT -> strings.appName
            WorkspaceSection.TOOLS -> strings.navAiTools
            WorkspaceSection.BUILDER -> strings.navAppBuilder
            WorkspaceSection.SETTINGS -> strings.navSettings
          }

          Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
              TopAppBar(
                title = {
                  Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                navigationIcon = {
                  IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("open_drawer_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Menu,
                      contentDescription = strings.navChatHistory,
                    )
                  }
                },
                actions = {
                  IconButton(
                    onClick = { viewModel.startNewChat() },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("topbar_new_chat_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.AddComment,
                      contentDescription = strings.navNewChat,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                  IconButton(
                    onClick = { viewModel.navigateTo(WorkspaceSection.TOOLS) },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("topbar_tools_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Build,
                      contentDescription = strings.navAiTools,
                    )
                  }
                  IconButton(
                    onClick = { viewModel.navigateTo(WorkspaceSection.BUILDER) },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("topbar_builder_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Web,
                      contentDescription = strings.navAppBuilder,
                    )
                  }
                  IconButton(
                    onClick = { viewModel.navigateTo(WorkspaceSection.SETTINGS) },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("topbar_settings_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Settings,
                      contentDescription = strings.navSettings,
                    )
                  }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
              )
            },
          ) { innerPadding ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              ActiveWorkspacePane(
                currentSection = currentSection,
                viewModel = viewModel,
                settings = settings,
                messages = messages,
                chatState = chatState,
                toolState = toolState,
                builderState = builderState,
                savedProjects = savedProjects,
                connectionStatus = connectionStatus,
                snackbarHostState = snackbarHostState,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActiveWorkspacePane(
  currentSection: WorkspaceSection,
  viewModel: WamdXViewModel,
  settings: com.example.data.local.UserSettings,
  messages: List<com.example.data.local.MessageEntity>,
  chatState: ChatUiState,
  toolState: ToolUiState,
  builderState: BuilderUiState,
  savedProjects: List<com.example.data.local.BuilderProjectEntity>,
  connectionStatus: com.example.data.remote.ConnectionStatus,
  snackbarHostState: SnackbarHostState,
) {
  when (currentSection) {
    WorkspaceSection.CHAT -> ChatScreen(
      messages = messages,
      chatState = chatState,
      settings = settings,
      snackbarHostState = snackbarHostState,
      onInputChanged = viewModel::updateChatInput,
      onSendMessage = viewModel::sendMessage,
      onAttachUri = viewModel::attachFileToChat,
      onClearAttachment = viewModel::clearChatAttachment,
      onSwitchProvider = viewModel::switchToSuggestedProvider,
      onOpenSettings = { viewModel.navigateTo(WorkspaceSection.SETTINGS) },
    )

    WorkspaceSection.TOOLS -> ToolsScreen(
      toolState = toolState,
      snackbarHostState = snackbarHostState,
      onSelectTool = viewModel::selectTool,
      onOpenBuilder = { viewModel.navigateTo(WorkspaceSection.BUILDER) },
      onUpdateInput = viewModel::updateToolInput,
      onUpdateTargetLanguage = viewModel::updateToolTargetLanguage,
      onAttachFile = viewModel::attachFileToTool,
      onRunTool = viewModel::executeActiveTool,
      onContinueInChat = viewModel::continueToolInChat,
    )

    WorkspaceSection.BUILDER -> BuilderScreen(
      builderState = builderState,
      savedProjects = savedProjects,
      snackbarHostState = snackbarHostState,
      onPromptChanged = viewModel::updateBuilderPrompt,
      onSubmitPrompt = viewModel::generateOrModifyBuilderApp,
      onSelectTab = viewModel::setBuilderTab,
      onToggleDesktop = viewModel::setBuilderDesktopMode,
      onSelectCodeFile = viewModel::setBuilderCodeFile,
      onEditSourceCode = viewModel::updateBuilderSourceCode,
      onNewProject = viewModel::startNewBuilderProject,
      onSaveProject = viewModel::saveCurrentBuilderProject,
      onOpenProject = viewModel::openBuilderProject,
      onDeleteProject = viewModel::deleteBuilderProject,
    )

    WorkspaceSection.SETTINGS -> SettingsScreen(
      settings = settings,
      connectionStatus = connectionStatus,
      snackbarHostState = snackbarHostState,
      onSelectLanguage = viewModel::setLanguage,
      onSelectTheme = viewModel::setThemeMode,
      onSelectProviderAndModel = viewModel::selectProviderAndModel,
      onTestConnection = viewModel::testActiveModelConnection,
      onSetMemoryEnabled = viewModel::setMemoryEnabled,
      onSaveMemoryNotes = viewModel::saveMemoryNotes,
      onClearMemory = viewModel::clearMemory,
      onClearAllConversations = viewModel::clearAllConversations,
    )
  }
}
