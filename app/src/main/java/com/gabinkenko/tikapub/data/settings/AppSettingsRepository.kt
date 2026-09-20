package com.gabinkenko.tikapub.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tikapub_settings")

data class AppSettings(
    val autoPublishEnabled: Boolean = false,
    val publishHour: Int = 18,
    val publishMinute: Int = 0,
    val privacyLevel: TikTokPrivacyLevel = TikTokPrivacyLevel.SELF_ONLY,
    val selectedCategories: Set<String> = emptySet(),
    val musicFolderUri: String? = null,
    val captionTemplate: String = "{quote}\n\n#motivation #citation #gabinkenko",
    val allowCommentsAllowDuet: Boolean = true,
)

/** Non-sensitive, user-facing preferences. Secrets/tokens live in [SecureTokenStore] instead. */
class AppSettingsRepository(context: Context) {

    private val store = context.dataStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            autoPublishEnabled = prefs[Keys.AUTO_PUBLISH_ENABLED] ?: false,
            publishHour = prefs[Keys.PUBLISH_HOUR] ?: 18,
            publishMinute = prefs[Keys.PUBLISH_MINUTE] ?: 0,
            privacyLevel = TikTokPrivacyLevel.fromApiValue(prefs[Keys.PRIVACY_LEVEL]),
            selectedCategories = prefs[Keys.SELECTED_CATEGORIES] ?: emptySet(),
            musicFolderUri = prefs[Keys.MUSIC_FOLDER_URI],
            captionTemplate = prefs[Keys.CAPTION_TEMPLATE]
                ?: "{quote}\n\n#motivation #citation #gabinkenko",
            allowCommentsAllowDuet = prefs[Keys.ALLOW_COMMENT_DUET] ?: true,
        )
    }

    suspend fun setAutoPublishEnabled(enabled: Boolean) {
        store.edit { it[Keys.AUTO_PUBLISH_ENABLED] = enabled }
    }

    suspend fun setPublishTime(hour: Int, minute: Int) {
        store.edit {
            it[Keys.PUBLISH_HOUR] = hour
            it[Keys.PUBLISH_MINUTE] = minute
        }
    }

    suspend fun setPrivacyLevel(level: TikTokPrivacyLevel) {
        store.edit { it[Keys.PRIVACY_LEVEL] = level.apiValue }
    }

    suspend fun setSelectedCategories(categories: Set<String>) {
        store.edit { it[Keys.SELECTED_CATEGORIES] = categories }
    }

    suspend fun setMusicFolderUri(uri: String?) {
        store.edit {
            if (uri == null) it.remove(Keys.MUSIC_FOLDER_URI) else it[Keys.MUSIC_FOLDER_URI] = uri
        }
    }

    suspend fun setCaptionTemplate(template: String) {
        store.edit { it[Keys.CAPTION_TEMPLATE] = template }
    }

    suspend fun setAllowCommentsAllowDuet(allow: Boolean) {
        store.edit { it[Keys.ALLOW_COMMENT_DUET] = allow }
    }

    private object Keys {
        val AUTO_PUBLISH_ENABLED = booleanPreferencesKey("auto_publish_enabled")
        val PUBLISH_HOUR = intPreferencesKey("publish_hour")
        val PUBLISH_MINUTE = intPreferencesKey("publish_minute")
        val PRIVACY_LEVEL = stringPreferencesKey("privacy_level")
        val SELECTED_CATEGORIES = stringSetPreferencesKey("selected_categories")
        val MUSIC_FOLDER_URI = stringPreferencesKey("music_folder_uri")
        val CAPTION_TEMPLATE = stringPreferencesKey("caption_template")
        val ALLOW_COMMENT_DUET = booleanPreferencesKey("allow_comment_duet")
    }
}
