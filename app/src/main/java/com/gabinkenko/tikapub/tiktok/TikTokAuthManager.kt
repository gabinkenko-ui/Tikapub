package com.gabinkenko.tikapub.tiktok

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.gabinkenko.tikapub.data.settings.SecureTokenStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Drives the TikTok Login Kit (OAuth 2.0 + PKCE) authorization-code flow.
 *
 * TikTok requires an HTTPS `redirect_uri` registered in the Developer Portal - it will not accept
 * a bare custom scheme. [REDIRECT_URI] should point at the small bridge page in `docs/index.html`
 * of this repo (deployed via GitHub Pages), which immediately forwards the browser to this app's
 * `tikapub://oauth-callback` deep link. See README.md for the full setup.
 */
class TikTokAuthManager(
    private val context: Context,
    private val tokenStore: SecureTokenStore,
) {
    /** Replace with the GitHub Pages URL you deploy from docs/, e.g. https://<user>.github.io/tikapub/ */
    var redirectUri: String = DEFAULT_REDIRECT_URI

    val requiredScopes = listOf("user.info.basic", "video.publish", "video.upload")

    fun buildAuthorizationIntentUri(): Uri {
        val clientKey = requireNotNull(tokenStore.clientKey) {
            "Renseigne d'abord ta Client Key TikTok dans les réglages."
        }

        val verifier = generateCodeVerifier()
        val challenge = codeChallengeS256(verifier)
        val state = generateState()
        tokenStore.pendingPkceVerifier = verifier
        tokenStore.pendingOAuthState = state

        return Uri.parse("https://www.tiktok.com/v2/auth/authorize/").buildUpon()
            .appendQueryParameter("client_key", clientKey)
            .appendQueryParameter("scope", requiredScopes.joinToString(","))
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    fun launchLogin() {
        val uri = buildAuthorizationIntentUri()
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }

    /** Result of parsing the `tikapub://oauth-callback?...` deep link. */
    sealed interface CallbackResult {
        data class Success(val code: String, val codeVerifier: String) : CallbackResult
        data class Error(val message: String) : CallbackResult
        data object NotACallback : CallbackResult
    }

    fun parseCallback(uri: Uri?): CallbackResult {
        if (uri == null || uri.scheme != "tikapub" || uri.host != "oauth-callback") {
            return CallbackResult.NotACallback
        }
        val error = uri.getQueryParameter("error")
        if (error != null) {
            tokenStore.clearPendingOAuth()
            return CallbackResult.Error(uri.getQueryParameter("error_description") ?: error)
        }
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val expectedState = tokenStore.pendingOAuthState
        val verifier = tokenStore.pendingPkceVerifier
        tokenStore.clearPendingOAuth()

        if (code.isNullOrBlank() || verifier.isNullOrBlank()) {
            return CallbackResult.Error("Réponse TikTok incomplète.")
        }
        if (expectedState == null || state != expectedState) {
            return CallbackResult.Error("État OAuth invalide (state mismatch) - relance la connexion.")
        }
        return CallbackResult.Success(code, verifier)
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return base64UrlEncode(bytes)
    }

    private fun generateState(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return base64UrlEncode(bytes)
    }

    private fun codeChallengeS256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64UrlEncode(digest)
    }

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    companion object {
        const val DEFAULT_REDIRECT_URI = "https://REPLACE_ME.github.io/tikapub/"
    }
}
