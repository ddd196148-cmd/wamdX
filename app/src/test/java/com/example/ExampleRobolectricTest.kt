package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BuilderProjectEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WamdXDatabase
import com.example.data.local.WorkspaceRepository
import com.example.ui.i18n.AppLanguage
import com.example.util.BuilderTools
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun resetPreferences() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    UserPreferencesRepository(context).resetToDefaults()
    val db = WamdXDatabase.getInstance(context)
    db.chatDao().deleteAllConversations()
  }

  @Test
  fun appNameAndArabicRtlDefaultAreVerified() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("WamdX AI Studio", context.getString(R.string.app_name))
    assertTrue(AppLanguage.AR.isRtl)
    assertFalse(AppLanguage.EN.isRtl)

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("مرحباً بك في WamdX AI Studio").assertIsDisplayed()
  }

  @Test
  fun switchBetweenArabicAndEnglishAndNavigateWorkspaces() {
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("topbar_settings_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("الإعدادات والتكوين").assertIsDisplayed()

    composeTestRule.onNodeWithTag("lang_en_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Settings & Configuration").assertIsDisplayed()

    composeTestRule.onNodeWithTag("topbar_tools_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Specialized AI Tools").assertIsDisplayed()

    composeTestRule.onNodeWithTag("topbar_builder_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Build Your First App or Website").assertIsDisplayed()

    composeTestRule.onNodeWithTag("topbar_settings_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lang_ar_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("الإعدادات والتكوين").assertIsDisplayed()
  }

  @Test
  fun sqliteWorkspacePersistenceAndBuilderBundleWork() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = WamdXDatabase.getInstance(context)
    val repo = WorkspaceRepository(db.chatDao(), db.builderProjectDao())

    val convId = repo.createConversation("اختبار الثقوب السوداء", "gemini", "gemini-3.5-flash")
    repo.addMessage(convId, "user", "اشرح الثقوب السوداء")
    repo.addMessage(convId, "assistant", "الثقب الأسود منطقة ذات جاذبية فائقة.", modelUsed = "gemini-3.5-flash")

    val msgs = repo.getMessagesOnce(convId)
    assertEquals(2, msgs.size)
    assertEquals("اشرح الثقوب السوداء", msgs[0].content)

    repo.deleteConversation(convId)
    assertTrue(repo.getMessagesOnce(convId).isEmpty())

    val projId = repo.saveProject(
      BuilderProjectEntity(
        name = "Calc",
        description = "Calculator",
        htmlCode = "<div id='calc'>0</div>",
        cssCode = "#calc { font-size: 24px; }",
        jsCode = "console.log('ready');",
        lastPrompt = "Build calc",
      )
    )
    val savedList = repo.projects.first()
    assertTrue(savedList.any { it.id == projId && it.name == "Calc" })
    repo.deleteProject(projId)

    val cleanReport = BuilderTools.validateProject(
      html = "<main id=\"app\"><button id=\"btn\">Click</button></main>",
      css = "body { background: #090910; color: #fff; }",
      js = "document.getElementById('btn').addEventListener('click', () => {});",
    )
    assertTrue(cleanReport.isClean)
  }
}
