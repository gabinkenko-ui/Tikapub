package com.gabinkenko.tikapub.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds everything sensitive: the TikTok app credentials you register on the TikTok Developer
 * Portal, and the OAuth tokens for @gabinkenko's account. Backed by AndroidX Security so it's
 * encrypted at rest with a key in the Android Keystore. Never log or transmit these values
 * anywhere other than https://open.tiktokapis.com.
 */
class SecureTokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "tikapub_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var clientKey: String?
        get() = prefs.getString(KEY_CLIENT_KEY, null)
        set(value) = prefs.edit().putString(KEY_CLIENT_KEY, value).apply()

    var clientSecret: String?
        get() = prefs.getString(KEY_CLIENT_SECRET, null)
        set(value) = prefs.edit().putString(KEY_CLIENT_SECRET, value).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var openId: String?
        get() = prefs.getString(KEY_OPEN_ID, null)
        set(value) = prefs.edit().putString(KEY_OPEN_ID, value).apply()

    var accessTokenExpiresAtMs: Long
        get() = prefs.getLong(KEY_ACCESS_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_ACCESS_EXPIRES, value).apply()

    var refreshTokenExpiresAtMs: Long
        get() = prefs.getLong(KEY_REFRESH_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_REFRESH_EXPIRES, value).apply()

    /** PKCE code_verifier for the authorization request currently in flight, if any. */
    var pendingPkceVerifier: String?
        get() = prefs.getString(KEY_PKCE_VERIFIER, null)
        set(value) = prefs.edit().putString(KEY_PKCE_VERIFIER, value).apply()

    var pendingOAuthState: String?
        get() = prefs.getString(KEY_OAUTH_STATE, null)
        set(value) = prefs.edit().putString(KEY_OAUTH_STATE, value).apply()

    val hasClientCredentials: Boolean get() = !clientKey.isNullOrBlank() && !clientSecret.isNullOrBlank()

    val isLinked: Boolean get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_OPEN_ID)
            .remove(KEY_ACCESS_EXPIRES)
            .remove(KEY_REFRESH_EXPIRES)
            .apply()
    }

    fun clearPendingOAuth() {
        prefs.edit().remove(KEY_PKCE_VERIFIER).remove(KEY_OAUTH_STATE).apply()
    }

    private companion object {
        const val KEY_CLIENT_KEY = "tiktok_client_key"
        const val KEY_CLIENT_SECRET = "tiktok_client_secret"
        const val KEY_ACCESS_TOKEN = "tiktok_access_token"
        const val KEY_REFRESH_TOKEN = "tiktok_refresh_token"
        const val KEY_OPEN_ID = "tiktok_open_id"
        const val KEY_ACCESS_EXPIRES = "tiktok_access_expires_at"
        const val KEY_REFRESH_EXPIRES = "tiktok_refresh_expires_at"
        const val KEY_PKCE_VERIFIER = "tiktok_pkce_verifier"
        const val KEY_OAUTH_STATE = "tiktok_oauth_state"
    }
}
