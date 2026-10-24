package com.example.ui.builder

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.BuilderProjectEntity
import com.example.ui.BuilderCodeFile
import com.example.ui.BuilderUiState
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.BuilderTools
import kotlinx.coroutines.launch

@Composable
fun BuilderScreen(
  builderState: BuilderUiState,
  savedProjects: List<BuilderProjectEntity>,
  snackbarHostState: SnackbarHostState,
  onPromptChanged: (String) -> Unit,
  onSubmitPrompt: (String?) -> Unit,
  onSelectTab: (Int) -> Unit,
  onToggleDesktop: (Boolean) -> Unit,
  onSelectCodeFile: (BuilderCodeFile) -> Unit,
  onEditSourceCode: (BuilderCodeFile, String) -> Unit,
  onNewProject: () -> Unit,
  onSaveProject: (String) -> Unit,
  onOpenProject: (BuilderProjectEntity) -> Unit,
  onDeleteProject: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val isAr = LocalAppLanguage.current == AppLanguage.AR
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()

  LaunchedEffect(builderState.bannerMessage) {
    val msg = builderState.bannerMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
    }
  }

  val exportHtmlLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/html")
  ) { uri ->
    if (uri != null) {
      scope.launch {
        BuilderTools.writeStandaloneHtmlToUri(
          context = context,
          uri = uri,
          title = builderState.projectName.ifBlank { "WamdX App" },
          html = builderState.htmlCode,
          css = builderState.cssCode,
          js = builderState.jsCode,
        ).onSuccess {
          snackbarHostState.showSnackbar(strings.builderExportSuccess)
        }
      }
    }
  }

  val exportZipLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip")
  ) { uri ->
    if (uri != null) {
      scope.launch {
        BuilderTools.writeProjectZipToUri(
          context = context,
          uri = uri,
          projectName = builderState.projectName.ifBlank { "WamdX_Project" },
          html = builderState.htmlCode,
          css = builderState.cssCode,
          js = builderState.jsCode,
        ).onSuccess {
          snackbarHostState.showSnackbar(strings.builderExportSuccess)
        }
      }
    }
  }

  val hasProjectContent = builderState.htmlCode.isNotBlank() || builderState.jsCode.isNotBlank()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .imePadding()
      .navigationBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top Header + Tabs
    Column(
      modifier = Modifier
        .widthIn(max = 960.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = builderState.projectName.ifBlank { strings.builderTitle },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = builderState.summary.ifBlank { strings.builderSubtitle },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = onNewProject,
            modifier = Modifier.testTag("builder_new_project_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.builderNewProject)
          }
          if (hasProjectContent) {
            Button(
              onClick = { onSaveProject(strings.builderProjectSaved) },
              modifier = Modifier.testTag("builder_save_project_button"),
            ) {
              Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(strings.builderSaveProject)
            }
          }
        }
      }

      Spacer(Modifier.height(8.dp))

      TabRow(selectedTabIndex = builderState.activeTabIndex) {
        Tab(
          selected = builderState.activeTabIndex == 0,
          onClick = { onSelectTab(0) },
          text = { Text(strings.builderTabPreview) },
          modifier = Modifier.testTag("builder_tab_preview"),
        )
        Tab(
          selected = builderState.activeTabIndex == 1,
          onClick = { onSelectTab(1) },
          text = { Text(strings.builderTabCode) },
          modifier = Modifier.testTag("builder_tab_code"),
        )
        Tab(
          selected = builderState.activeTabIndex == 2,
          onClick = { onSelectTab(2) },
          text = { Text("${strings.builderTabProjects} (${savedProjects.size})") },
          modifier = Modifier.testTag("builder_tab_saved"),
        )
      }
    }

    // Error Banner if any
    val errText = if (isAr) builderState.errorAr else builderState.errorEn
    if (errText != null) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
          .widthIn(max = 960.dp)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
      ) {
        Text(
          text = errText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    // Main Tab Body
    Box(
      modifier = Modifier
        .weight(1f)
        .widthIn(max = 960.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      when (builderState.activeTabIndex) {
        0 -> BuilderPreviewPane(
          builderState = builderState,
          hasContent = hasProjectContent,
          isAr = isAr,
          onToggleDesktop = onToggleDesktop,
          onStarterClick = { starterPrompt -> onSubmitPrompt(starterPrompt) },
          onExportHtml = {
            val slug = builderState.projectName.ifBlank { "wamdx_app" }.replace(" ", "_")
            exportHtmlLauncher.launch("$slug.html")
          },
          onExportZip = {
            val slug = builderState.projectName.ifBlank { "wamdx_project" }.replace(" ", "_")
            exportZipLauncher.launch("$slug.zip")
          },
          onShareZip = {
            scope.launch {
              BuilderTools.createShareableZipIntent(
                context = context,
                projectName = builderState.projectName.ifBlank { "WamdX_Project" },
                html = builderState.htmlCode,
                css = builderState.cssCode,
                js = builderState.jsCode,
              ).onSuccess { chooser -> context.startActivity(chooser) }
            }
          },
        )

        1 -> BuilderSourceCodePane(
          builderState = builderState,
          onSelectFile = onSelectCodeFile,
          onEditCode = onEditSourceCode,
          onCopyCode = { code ->
            clipboard.setText(AnnotatedString(code))
            scope.launch { snackbarHostState.showSnackbar(strings.copiedToast) }
          },
        )

        2 -> SavedProjectsPane(
          projects = savedProjects,
          onOpen = onOpenProject,
          onDelete = onDeleteProject,
        )
      }
    }

    // Bottom Natural-Language Builder Prompt Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .widthIn(max = 960.dp)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        OutlinedTextField(
          value = builderState.promptInput,
          onValueChange = onPromptChanged,
          placeholder = { Text(strings.builderPromptHint, style = MaterialTheme.typography.bodySmall) },
          maxLines = 3,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .weight(1f)
            .testTag("builder_prompt_input"),
        )
        Button(
          onClick = { onSubmitPrompt(null) },
          enabled = !builderState.isGenerating && builderState.promptInput.isNotBlank(),
          modifier = Modifier
            .height(50.dp)
            .testTag("builder_submit_button"),
        ) {
          if (builderState.isGenerating) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Text(if (hasProjectContent) strings.builderModifyBtn else strings.builderGenerateBtn)
          }
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BuilderPreviewPane(
  builderState: BuilderUiState,
  hasContent: Boolean,
  isAr: Boolean,
  onToggleDesktop: (Boolean) -> Unit,
  onStarterClick: (String) -> Unit,
  onExportHtml: () -> Unit,
  onExportZip: () -> Unit,
  onShareZip: () -> Unit,
) {
  val strings = LocalAppStrings.current

  if (builderState.isGenerating) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(
          text = strings.builderGeneratingStatus,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
    return
  }

  if (!hasContent) {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item {
        Surface(
          shape = MaterialTheme.shapes.extraLarge,
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Outlined.Web,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = strings.builderEmptyPreviewTitle,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
              text = strings.builderEmptyPreviewDesc,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
              AssistChip(
                onClick = { onStarterClick(strings.builderStarterCalcPrompt) },
                label = { Text(strings.builderStarterCalc) },
              )
              AssistChip(
                onClick = { onStarterClick(strings.builderStarterGamePrompt) },
                label = { Text(strings.builderStarterGame) },
              )
              AssistChip(
                onClick = { onStarterClick(strings.builderStarterDashboardPrompt) },
                label = { Text(strings.builderStarterDashboard) },
              )
              AssistChip(
                onClick = { onStarterClick(strings.builderStarterPortfolioPrompt) },
                label = { Text(strings.builderStarterPortfolio) },
              )
            }
          }
        }
      }
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Viewport Mode + Export Toolbar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = !builderState.isDesktopPreview,
        onClick = { onToggleDesktop(false) },
        label = { Text(strings.builderMobileMode) },
        leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.testTag("toggle_mobile_mode"),
      )
      FilterChip(
        selected = builderState.isDesktopPreview,
        onClick = { onToggleDesktop(true) },
        label = { Text(strings.builderDesktopMode) },
        leadingIcon = { Icon(Icons.Filled.DesktopWindows, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.testTag("toggle_desktop_mode"),
      )
      OutlinedButton(
        onClick = onExportHtml,
        modifier = Modifier.testTag("export_html_button"),
      ) {
        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(strings.builderExportHtml)
      }
      OutlinedButton(
        onClick = onExportZip,
        modifier = Modifier.testTag("export_zip_button"),
      ) {
        Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(strings.builderExportZip)
      }
      TextButton(onClick = onShareZip) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(strings.builderShareZip)
      }
    }

    // Validation Status Bar
    val report = builderState.validationReport
    if (report != null) {
      val issues = if (isAr) report.issuesAr else report.issuesEn
      Surface(
        shape = MaterialTheme.shapes.small,
        color = if (report.isClean) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Icon(
            imageVector = if (report.isClean) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (report.isClean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = if (report.isClean) strings.builderValidationClean
            else "${strings.builderValidationIssues} ${issues.joinToString(" • ")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    val bundledHtml = remember(
      builderState.htmlCode,
      builderState.cssCode,
      builderState.jsCode,
      builderState.isDesktopPreview,
      builderState.previewReloadNonce,
    ) {
      BuilderTools.buildStandaloneHtml(
        title = builderState.projectName.ifBlank { "WamdX Preview" },
        htmlBodyOrDoc = builderState.htmlCode,
        cssCode = builderState.cssCode,
        jsCode = builderState.jsCode,
        desktopViewport = builderState.isDesktopPreview,
      )
    }

    // Security-Hardened Sandboxed WebView
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
    ) {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = false
              allowFileAccess = false
              allowContentAccess = false
              javaScriptCanOpenWindowsAutomatically = false
              cacheMode = WebSettings.LOAD_NO_CACHE
              useWideViewPort = true
              loadWithOverviewMode = true
              builtInZoomControls = true
              displayZoomControls = false
            }
            webViewClient = object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
              ): Boolean {
                // Prevent navigating away from sandboxed preview
                return true
              }
            }
          }
        },
        update = { webView ->
          webView.loadDataWithBaseURL(
            "about:blank",
            bundledHtml,
            "text/html",
            "UTF-8",
            null,
          )
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("builder_sandbox_webview"),
      )
    }
  }
}

@Composable
private fun BuilderSourceCodePane(
  builderState: BuilderUiState,
  onSelectFile: (BuilderCodeFile) -> Unit,
  onEditCode: (BuilderCodeFile, String) -> Unit,
  onCopyCode: (String) -> Unit,
) {
  val strings = LocalAppStrings.current
  val currentCode = when (builderState.selectedCodeFile) {
    BuilderCodeFile.HTML -> builderState.htmlCode
    BuilderCodeFile.CSS -> builderState.cssCode
    BuilderCodeFile.JS -> builderState.jsCode
    BuilderCodeFile.STANDALONE -> BuilderTools.buildStandaloneHtml(
      title = builderState.projectName.ifBlank { "WamdX App" },
      htmlBodyOrDoc = builderState.htmlCode,
      cssCode = builderState.cssCode,
      jsCode = builderState.jsCode,
    )
  }

  // Strictly Left-to-Right (LTR) for code editing regardless of Arabic RTL surrounding interface
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = builderState.selectedCodeFile == BuilderCodeFile.HTML,
            onClick = { onSelectFile(BuilderCodeFile.HTML) },
            label = { Text("index.html") },
          )
          FilterChip(
            selected = builderState.selectedCodeFile == BuilderCodeFile.CSS,
            onClick = { onSelectFile(BuilderCodeFile.CSS) },
            label = { Text("styles.css") },
          )
          FilterChip(
            selected = builderState.selectedCodeFile == BuilderCodeFile.JS,
            onClick = { onSelectFile(BuilderCodeFile.JS) },
            label = { Text("app.js") },
          )
          FilterChip(
            selected = builderState.selectedCodeFile == BuilderCodeFile.STANDALONE,
            onClick = { onSelectFile(BuilderCodeFile.STANDALONE) },
            label = { Text("standalone.html") },
          )
        }
        TextButton(onClick = { onCopyCode(currentCode) }) {
          Icon(Icons.Outlined.ContentCopy, contentDescription = strings.copyCode, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(strings.copyCode)
        }
      }

      Spacer(Modifier.height(6.dp))

      OutlinedTextField(
        value = currentCode,
        onValueChange = { updated ->
          if (builderState.selectedCodeFile != BuilderCodeFile.STANDALONE) {
            onEditCode(builderState.selectedCodeFile, updated)
          }
        },
        readOnly = builderState.selectedCodeFile == BuilderCodeFile.STANDALONE,
        textStyle = TextStyle(
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 13.sp,
          lineHeight = 19.sp,
        ),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("builder_code_editor"),
      )
    }
  }
}

@Composable
private fun SavedProjectsPane(
  projects: List<BuilderProjectEntity>,
  onOpen: (BuilderProjectEntity) -> Unit,
  onDelete: (Long) -> Unit,
) {
  val strings = LocalAppStrings.current

  if (projects.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = strings.builderNoProjects,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(vertical = 8.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    items(projects, key = { it.id }) { proj ->
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = proj.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (proj.description.isNotBlank()) {
              Text(
                text = proj.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
              onClick = { onOpen(proj) },
              modifier = Modifier.testTag("open_project_${proj.id}"),
            ) {
              Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(strings.builderOpenProject)
            }
            IconButton(
              onClick = { onDelete(proj.id) },
              modifier = Modifier
                .size(48.dp)
                .testTag("delete_project_${proj.id}"),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = strings.builderDeleteProject,
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }
    }
  }
}
