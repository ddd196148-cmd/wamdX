package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wamdx_prefs")

data class UserSettings(
  val language: AppLanguage = AppLanguage.AR, // Arabic MUST be default on first open
  val themeMode: AppThemeMode = AppThemeMode.DARK,
  val providerId: String = "gemini",
  val modelId: String = "gemini-3.5-flash",
  val memoryEnabled: Boolean = false,
  val memoryNotes: String = "",
)

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val LANGUAGE = stringPreferencesKey("language_code")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val PROVIDER_ID = stringPreferencesKey("ai_provider_id")
    val MODEL_ID = stringPreferencesKey("ai_model_id")
    val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    val MEMORY_NOTES = stringPreferencesKey("memory_notes")
  }

  private val diskFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
    UserSettings(
      language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.AR.code),
      themeMode = AppThemeMode.fromId(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.id),
      providerId = prefs[Keys.PROVIDER_ID] ?: "gemini",
      modelId = prefs[Keys.MODEL_ID] ?: "gemini-3.5-flash",
      memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: false,
      memoryNotes = prefs[Keys.MEMORY_NOTES] ?: "",
    )
  }

  val settingsFlow: Flow<UserSettings> = combine(diskFlow, optimisticPatch) { disk, patch ->
    UserSettings(
      language = patch.language ?: disk.language,
      themeMode = patch.themeMode ?: disk.themeMode,
      providerId = patch.providerId ?: disk.providerId,
      modelId = patch.modelId ?: disk.modelId,
      memoryEnabled = patch.memoryEnabled ?: disk.memoryEnabled,
      memoryNotes = patch.memoryNotes ?: disk.memoryNotes,
    )
  }

  suspend fun setLanguage(language: AppLanguage) {
    optimisticPatch.value = optimisticPatch.value.copy(language = language)
    context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun setThemeMode(themeMode: AppThemeMode) {
    optimisticPatch.value = optimisticPatch.value.copy(themeMode = themeMode)
    context.dataStore.edit { it[Keys.THEME_MODE] = themeMode.id }
  }

  suspend fun setProviderAndModel(providerId: String, modelId: String) {
    optimisticPatch.value = optimisticPatch.value.copy(providerId = providerId, modelId = modelId)
    context.dataStore.edit {
      it[Keys.PROVIDER_ID] = providerId
      it[Keys.MODEL_ID] = modelId
    }
  }

  suspend fun setMemoryEnabled(enabled: Boolean) {
    optimisticPatch.value = optimisticPatch.value.copy(memoryEnabled = enabled)
    context.dataStore.edit { it[Keys.MEMORY_ENABLED] = enabled }
  }

  suspend fun saveMemoryNotes(notes: String) {
    val clean = notes.trim()
    optimisticPatch.value = optimisticPatch.value.copy(memoryNotes = clean)
    context.dataStore.edit { it[Keys.MEMORY_NOTES] = clean }
  }

  suspend fun clearMemory() {
    optimisticPatch.value = optimisticPatch.value.copy(memoryNotes = "", memoryEnabled = false)
    context.dataStore.edit {
      it[Keys.MEMORY_NOTES] = ""
      it[Keys.MEMORY_ENABLED] = false
    }
  }

  suspend fun resetToDefaults() {
    optimisticPatch.value = SettingsPatch()
    context.dataStore.edit { it.clear() }
  }

  private data class SettingsPatch(
    val language: AppLanguage? = null,
    val themeMode: AppThemeMode? = null,
    val providerId: String? = null,
    val modelId: String? = null,
    val memoryEnabled: Boolean? = null,
    val memoryNotes: String? = null,
  )

  companion object {
    private val optimisticPatch = MutableStateFlow(SettingsPatch())
  }
}
