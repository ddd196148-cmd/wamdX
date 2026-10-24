package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BuilderProjectEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.local.WamdXDatabase
import com.example.data.local.WorkspaceRepository
import com.example.data.remote.AiCatalog
import com.example.data.remote.AiOutcome
import com.example.data.remote.AiProviderType
import com.example.data.remote.AiRepository
import com.example.data.remote.ConnectionStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppThemeMode
import com.example.util.AttachmentHelper
import com.example.util.BuilderTools
import com.example.util.CodeValidationReport
import com.example.util.PickedAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkspaceSection {
  CHAT,
  TOOLS,
  BUILDER,
  SETTINGS,
}

enum class AiToolId {
  WRITING,
  TRANSLATE,
  SUMMARIZE,
  IMPROVE,
  CODE,
  MATH_SCIENCE,
  IDEAS,
  FILE_ANALYSIS,
}

enum class BuilderCodeFile {
  HTML,
  CSS,
  JS,
  STANDALONE,
}

data class ChatUiState(
  val inputText: String = "",
  val pendingAttachment: PickedAttachment? = null,
  val isSending: Boolean = false,
  val errorAr: String? = null,
  val errorEn: String? = null,
  val fallbackProvider: AiProviderType? = null,
  val lastFailedPrompt: String? = null,
)

data class ToolUiState(
  val activeTool: AiToolId? = null,
  val inputText: String = "",
  val targetLanguage: String = "العربية (Arabic)",
  val attachment: PickedAttachment? = null,
  val isRunning: Boolean = false,
  val resultText: String? = null,
  val errorAr: String? = null,
  val errorEn: String? = null,
)

data class BuilderUiState(
  val currentProjectId: Long = 0L,
  val projectName: String = "",
  val summary: String = "",
  val htmlCode: String = "",
  val cssCode: String = "",
  val jsCode: String = "",
  val promptInput: String = "",
  val isGenerating: Boolean = false,
  val isDesktopPreview: Boolean = false,
  val activeTabIndex: Int = 0, // 0 = Preview, 1 = Code, 2 = Saved Projects
  val selectedCodeFile: BuilderCodeFile = BuilderCodeFile.HTML,
  val previewReloadNonce: Int = 0,
  val validationReport: CodeValidationReport? = null,
  val errorAr: String? = null,
  val errorEn: String? = null,
  val bannerMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WamdXViewModel(
  private val workspaceRepo: WorkspaceRepository,
  private val prefsRepo: UserPreferencesRepository,
  private val aiRepo: AiRepository,
) : ViewModel() {

  val settings: StateFlow<UserSettings> = prefsRepo.settingsFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = UserSettings(),
  )

  val conversations: StateFlow<List<ConversationEntity>> = workspaceRepo.conversations.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList(),
  )

  val savedProjects: StateFlow<List<BuilderProjectEntity>> = workspaceRepo.projects.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList(),
  )

  private val _currentSection = MutableStateFlow(WorkspaceSection.CHAT)
  val currentSection: StateFlow<WorkspaceSection> = _currentSection.asStateFlow()

  private val _activeConversationId = MutableStateFlow<Long?>(null)
  val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

  val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
    .flatMapLatest { id ->
      if (id == null) flowOf(emptyList()) else workspaceRepo.observeMessages(id)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  private val _chatState = MutableStateFlow(ChatUiState())
  val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

  private val _toolState = MutableStateFlow(ToolUiState())
  val toolState: StateFlow<ToolUiState> = _toolState.asStateFlow()

  private val _builderState = MutableStateFlow(BuilderUiState())
  val builderState: StateFlow<BuilderUiState> = _builderState.asStateFlow()

  private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unverified)
  val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

  fun navigateTo(section: WorkspaceSection) {
    _currentSection.value = section
  }

  // --- Conversation Controls ---
  fun startNewChat() {
    _activeConversationId.value = null
    _chatState.update { ChatUiState() }
    _currentSection.value = WorkspaceSection.CHAT
  }

  fun openConversation(conversationId: Long) {
    _activeConversationId.value = conversationId
    _chatState.update { it.copy(errorAr = null, errorEn = null) }
    _currentSection.value = WorkspaceSection.CHAT
  }

  fun deleteConversation(conversationId: Long) {
    viewModelScope.launch {
      workspaceRepo.deleteConversation(conversationId)
      if (_activeConversationId.value == conversationId) {
        _activeConversationId.value = null
      }
    }
  }

  fun clearAllConversations() {
    viewModelScope.launch {
      workspaceRepo.deleteAllConversations()
      _activeConversationId.value = null
    }
  }

  // --- Chat Composer & Messaging ---
  fun updateChatInput(text: String) {
    _chatState.update { it.copy(inputText = text, errorAr = null, errorEn = null) }
  }

  fun attachFileToChat(context: Context, uri: Uri) {
    viewModelScope.launch {
      val result = AttachmentHelper.readAttachment(context, uri)
      result.fold(
        onSuccess = { picked ->
          _chatState.update { it.copy(pendingAttachment = picked, errorAr = null, errorEn = null) }
        },
        onFailure = { err ->
          _chatState.update {
            it.copy(
              errorAr = "تعذّر قراءة الملف: ${err.message}",
              errorEn = "Could not attach file: ${err.message}",
            )
          }
        },
      )
    }
  }

  fun clearChatAttachment() {
    _chatState.update { it.copy(pendingAttachment = null) }
  }

  fun sendMessage(overridePrompt: String? = null) {
    val state = _chatState.value
    if (state.isSending) return
    val text = (overridePrompt ?: state.inputText).trim()
    val attachment = state.pendingAttachment
    if (text.isEmpty() && attachment == null) return

    val promptToSend = text.ifEmpty {
      if (settings.value.language == AppLanguage.AR) "حلّل هذا الملف بالتفصيل." else "Analyze this attached file in detail."
    }

    _chatState.update {
      it.copy(
        inputText = "",
        pendingAttachment = null,
        isSending = true,
        errorAr = null,
        errorEn = null,
        fallbackProvider = null,
        lastFailedPrompt = null,
      )
    }

    viewModelScope.launch {
      val currentSettings = settings.value
      val convId = _activeConversationId.value ?: run {
        val shortTitle = promptToSend.lineSequence().firstOrNull()?.take(42) ?: "Chat"
        val createdId = workspaceRepo.createConversation(
          title = shortTitle,
          providerId = currentSettings.providerId,
          modelId = currentSettings.modelId,
        )
        _activeConversationId.value = createdId
        createdId
      }

      val previousHistory = workspaceRepo.getMessagesOnce(convId)

      workspaceRepo.addMessage(
        conversationId = convId,
        role = "user",
        content = promptToSend,
        attachmentName = attachment?.name,
        attachmentMimeType = attachment?.mimeType,
      )

      when (
        val outcome = aiRepo.sendChatTurn(
          modelId = currentSettings.modelId,
          history = previousHistory,
          userPrompt = promptToSend,
          attachment = attachment,
          memoryEnabled = currentSettings.memoryEnabled,
          memoryNotes = currentSettings.memoryNotes,
        )
      ) {
        is AiOutcome.Success -> {
          _connectionStatus.value = ConnectionStatus.Connected(outcome.modelUsed, outcome.latencyMs)
          workspaceRepo.addMessage(
            conversationId = convId,
            role = "assistant",
            content = outcome.data,
            modelUsed = outcome.modelUsed,
          )
          _chatState.update { it.copy(isSending = false) }
        }

        is AiOutcome.Error -> {
          if (outcome.isRateLimit) {
            _connectionStatus.value = ConnectionStatus.RateLimited(
              message = outcome.messageEn,
              fallbackProvider = outcome.suggestedFallback,
            )
          }
          _chatState.update {
            it.copy(
              isSending = false,
              errorAr = outcome.messageAr,
              errorEn = outcome.messageEn,
              fallbackProvider = outcome.suggestedFallback,
              lastFailedPrompt = promptToSend,
            )
          }
        }
      }
    }
  }

  fun switchToSuggestedProvider(provider: AiProviderType) {
    val firstModel = AiCatalog.modelsForProvider(provider).firstOrNull() ?: return
    selectProviderAndModel(provider.id, firstModel.id)
  }

  // --- Specialized AI Tools ---
  fun selectTool(toolId: AiToolId?) {
    _toolState.value = ToolUiState(activeTool = toolId)
  }

  fun updateToolInput(text: String) {
    _toolState.update { it.copy(inputText = text, errorAr = null, errorEn = null) }
  }

  fun updateToolTargetLanguage(lang: String) {
    _toolState.update { it.copy(targetLanguage = lang) }
  }

  fun attachFileToTool(context: Context, uri: Uri) {
    viewModelScope.launch {
      val result = AttachmentHelper.readAttachment(context, uri)
      result.fold(
        onSuccess = { picked ->
          _toolState.update { it.copy(attachment = picked, errorAr = null, errorEn = null) }
        },
        onFailure = { err ->
          _toolState.update {
            it.copy(
              errorAr = "تعذّر قراءة الملف: ${err.message}",
              errorEn = "Could not attach file: ${err.message}",
            )
          }
        },
      )
    }
  }

  fun executeActiveTool() {
    val tState = _toolState.value
    val tool = tState.activeTool ?: return
    if (tState.isRunning) return
    if (tState.inputText.isBlank() && tState.attachment == null) return

    val roleInstruction = when (tool) {
      AiToolId.WRITING -> "Act as an expert creative and professional writer. Produce well-structured, engaging text."
      AiToolId.TRANSLATE -> "Translate the user's text accurately and naturally into ${tState.targetLanguage}. Preserve tone and formatting."
      AiToolId.SUMMARIZE -> "Summarize the user's text into a concise overview followed by clear bullet-point key takeaways."
      AiToolId.IMPROVE -> "Proofread, fix grammar, and rewrite the user's text for maximum clarity, flow, and polish. Show the improved version first, then briefly list key improvements."
      AiToolId.CODE -> "Act as a senior software engineer. Provide clean, well-commented, working code inside fenced markdown blocks and explain the solution."
      AiToolId.MATH_SCIENCE -> "Act as a patient STEM tutor. Solve or explain step by step, showing formulas, reasoning, and final verification clearly."
      AiToolId.IDEAS -> "Generate 8–10 original, actionable ideas organized with catchy titles and practical execution steps."
      AiToolId.FILE_ANALYSIS -> "Carefully analyze the attached file/image and answer the user's request with structured insights."
    }

    val promptText = tState.inputText.ifBlank {
      if (settings.value.language == AppLanguage.AR) "حلّل هذا المرفق واستخرج أهم المعلومات."
      else "Analyze this attachment and extract key insights."
    }

    _toolState.update { it.copy(isRunning = true, errorAr = null, errorEn = null, resultText = null) }

    viewModelScope.launch {
      val currentSettings = settings.value
      when (
        val outcome = aiRepo.sendChatTurn(
          modelId = currentSettings.modelId,
          history = emptyList(),
          userPrompt = promptText,
          attachment = tState.attachment,
          memoryEnabled = currentSettings.memoryEnabled,
          memoryNotes = currentSettings.memoryNotes,
          extraRoleInstruction = roleInstruction,
        )
      ) {
        is AiOutcome.Success -> {
          _connectionStatus.value = ConnectionStatus.Connected(outcome.modelUsed, outcome.latencyMs)
          _toolState.update { it.copy(isRunning = false, resultText = outcome.data) }
        }
        is AiOutcome.Error -> {
          _toolState.update {
            it.copy(
              isRunning = false,
              errorAr = outcome.messageAr,
              errorEn = outcome.messageEn,
            )
          }
        }
      }
    }
  }

  fun continueToolInChat() {
    val tState = _toolState.value
    val result = tState.resultText ?: return
    val userReq = tState.inputText.ifBlank { tState.attachment?.name ?: "Tool Task" }
    viewModelScope.launch {
      val s = settings.value
      val convId = workspaceRepo.createConversation(
        title = userReq.take(40),
        providerId = s.providerId,
        modelId = s.modelId,
      )
      workspaceRepo.addMessage(convId, "user", userReq, tState.attachment?.name, tState.attachment?.mimeType)
      workspaceRepo.addMessage(convId, "assistant", result, modelUsed = s.modelId)
      _activeConversationId.value = convId
      _currentSection.value = WorkspaceSection.CHAT
    }
  }

  // --- App & Website Builder ---
  fun updateBuilderPrompt(text: String) {
    _builderState.update { it.copy(promptInput = text, errorAr = null, errorEn = null, bannerMessage = null) }
  }

  fun setBuilderTab(index: Int) {
    _builderState.update { it.copy(activeTabIndex = index) }
  }

  fun setBuilderDesktopMode(desktop: Boolean) {
    _builderState.update { it.copy(isDesktopPreview = desktop, previewReloadNonce = it.previewReloadNonce + 1) }
  }

  fun setBuilderCodeFile(file: BuilderCodeFile) {
    _builderState.update { it.copy(selectedCodeFile = file) }
  }

  fun updateBuilderSourceCode(file: BuilderCodeFile, updatedCode: String) {
    _builderState.update { state ->
      val next = when (file) {
        BuilderCodeFile.HTML -> state.copy(htmlCode = updatedCode)
        BuilderCodeFile.CSS -> state.copy(cssCode = updatedCode)
        BuilderCodeFile.JS -> state.copy(jsCode = updatedCode)
        BuilderCodeFile.STANDALONE -> state
      }
      next.copy(
        validationReport = BuilderTools.validateProject(next.htmlCode, next.cssCode, next.jsCode),
        previewReloadNonce = next.previewReloadNonce + 1,
      )
    }
  }

  fun startNewBuilderProject() {
    _builderState.value = BuilderUiState()
  }

  fun generateOrModifyBuilderApp(customPrompt: String? = null) {
    val bState = _builderState.value
    if (bState.isGenerating) return
    val prompt = (customPrompt ?: bState.promptInput).trim()
    if (prompt.isEmpty()) return

    _builderState.update {
      it.copy(
        promptInput = if (customPrompt != null) "" else "",
        isGenerating = true,
        errorAr = null,
        errorEn = null,
        bannerMessage = null,
        activeTabIndex = 0,
      )
    }

    viewModelScope.launch {
      val s = settings.value
      when (
        val outcome = aiRepo.generateOrModifyWebApp(
          modelId = s.modelId,
          userInstruction = prompt,
          currentHtml = bState.htmlCode,
          currentCss = bState.cssCode,
          currentJs = bState.jsCode,
          isArabicUi = s.language == AppLanguage.AR,
        )
      ) {
        is AiOutcome.Success -> {
          _connectionStatus.value = ConnectionStatus.Connected(outcome.modelUsed, outcome.latencyMs)
          val payload = outcome.data
          val report = BuilderTools.validateProject(payload.htmlCode, payload.cssCode, payload.jsCode)
          _builderState.update { prev ->
            prev.copy(
              projectName = payload.projectName.ifBlank { prev.projectName.ifBlank { "WamdX App" } },
              summary = payload.summary,
              htmlCode = payload.htmlCode,
              cssCode = payload.cssCode,
              jsCode = payload.jsCode,
              isGenerating = false,
              validationReport = report,
              previewReloadNonce = prev.previewReloadNonce + 1,
            )
          }
        }
        is AiOutcome.Error -> {
          _builderState.update {
            it.copy(
              isGenerating = false,
              promptInput = prompt,
              errorAr = outcome.messageAr,
              errorEn = outcome.messageEn,
            )
          }
        }
      }
    }
  }

  fun saveCurrentBuilderProject(savedToastMessage: String) {
    val bState = _builderState.value
    if (bState.htmlCode.isBlank() && bState.jsCode.isBlank()) return
    viewModelScope.launch {
      val entity = BuilderProjectEntity(
        id = bState.currentProjectId,
        name = bState.projectName.ifBlank { "WamdX Web Project" },
        description = bState.summary,
        htmlCode = bState.htmlCode,
        cssCode = bState.cssCode,
        jsCode = bState.jsCode,
        lastPrompt = bState.summary,
      )
      val newId = workspaceRepo.saveProject(entity)
      _builderState.update {
        it.copy(currentProjectId = newId, bannerMessage = savedToastMessage)
      }
    }
  }

  fun openBuilderProject(project: BuilderProjectEntity) {
    val report = BuilderTools.validateProject(project.htmlCode, project.cssCode, project.jsCode)
    _builderState.update {
      it.copy(
        currentProjectId = project.id,
        projectName = project.name,
        summary = project.description,
        htmlCode = project.htmlCode,
        cssCode = project.cssCode,
        jsCode = project.jsCode,
        activeTabIndex = 0,
        validationReport = report,
        previewReloadNonce = it.previewReloadNonce + 1,
        errorAr = null,
        errorEn = null,
      )
    }
  }

  fun deleteBuilderProject(projectId: Long) {
    viewModelScope.launch {
      workspaceRepo.deleteProject(projectId)
      if (_builderState.value.currentProjectId == projectId) {
        _builderState.update { it.copy(currentProjectId = 0L) }
      }
    }
  }

  fun postBuilderBanner(message: String) {
    _builderState.update { it.copy(bannerMessage = message) }
  }

  // --- Settings & Preferences ---
  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { prefsRepo.setLanguage(language) }
  }

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { prefsRepo.setThemeMode(mode) }
  }

  fun selectProviderAndModel(providerId: String, modelId: String) {
    _connectionStatus.value = ConnectionStatus.Unverified
    viewModelScope.launch { prefsRepo.setProviderAndModel(providerId, modelId) }
  }

  fun testActiveModelConnection() {
    _connectionStatus.value = ConnectionStatus.Checking
    viewModelScope.launch {
      val modelId = settings.value.modelId
      _connectionStatus.value = aiRepo.verifyConnection(modelId)
    }
  }

  fun setMemoryEnabled(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setMemoryEnabled(enabled) }
  }

  fun saveMemoryNotes(notes: String) {
    viewModelScope.launch { prefsRepo.saveMemoryNotes(notes) }
  }

  fun clearMemory() {
    viewModelScope.launch { prefsRepo.clearMemory() }
  }

  companion object {
    fun provideFactory(context: Context): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val appContext = context.applicationContext
          val db = WamdXDatabase.getInstance(appContext)
          val workspaceRepo = WorkspaceRepository(db.chatDao(), db.builderProjectDao())
          val prefsRepo = UserPreferencesRepository(appContext)
          val aiRepo = AiRepository()
          return WamdXViewModel(workspaceRepo, prefsRepo, aiRepo) as T
        }
      }
  }
}
