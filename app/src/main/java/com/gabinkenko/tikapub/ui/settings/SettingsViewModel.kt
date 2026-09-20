package com.gabinkenko.tikapub.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabinkenko.tikapub.AppContainer
import com.gabinkenko.tikapub.data.settings.AppSettings
import com.gabinkenko.tikapub.data.settings.TikTokPrivacyLevel
import com.gabinkenko.tikapub.tiktok.AuthEventBus
import com.gabinkenko.tikapub.tiktok.TikTokAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val categories: List<String> = emptyList(),
    val clientKey: String = "",
    val clientSecret: String = "",
    val redirectUri: String = "",
    val isLinked: Boolean = false,
    val isConnecting: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val extra = MutableStateFlow(
        SettingsUiState(
            clientKey = container.tokenStore.clientKey.orEmpty(),
            clientSecret = container.tokenStore.clientSecret.orEmpty(),
            redirectUri = container.authManager.redirectUri,
            isLinked = container.tokenStore.isLinked,
        ),
    )

    val uiState: StateFlow<SettingsUiState> = extra.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                extra.value = extra.value.copy(settings = settings)
            }
        }
        viewModelScope.launch {
            container.quoteDao.observeCategories().collect { cats ->
                extra.value = extra.value.copy(categories = cats)
            }
        }
        viewModelScope.launch {
            AuthEventBus.events.collect { result -> handleCallback(result) }
        }
    }

    fun onClientKeyChanged(value: String) {
        extra.value = extra.value.copy(clientKey = value)
        container.tokenStore.clientKey = value.trim()
    }

    fun onClientSecretChanged(value: String) {
        extra.value = extra.value.copy(clientSecret = value)
        container.tokenStore.clientSecret = value.trim()
    }

    fun onRedirectUriChanged(value: String) {
        extra.value = extra.value.copy(redirectUri = value)
        container.authManager.redirectUri = value.trim()
    }

    fun startLogin(): Boolean {
        if (extra.value.clientKey.isBlank() || extra.value.clientSecret.isBlank()) {
            extra.value = extra.value.copy(statusMessage = "Renseigne la Client Key et le Client Secret d'abord.", isError = true)
            return false
        }
        extra.value = extra.value.copy(isConnecting = true, statusMessage = null, isError = false)
        return true
    }

    fun disconnect() {
        container.tokenStore.clearTokens()
        extra.value = extra.value.copy(isLinked = false, statusMessage = "Compte TikTok déconnecté.", isError = false)
    }

    fun setPrivacyLevel(level: TikTokPrivacyLevel) {
        viewModelScope.launch { container.settingsRepository.setPrivacyLevel(level) }
    }

    fun setPublishTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            container.settingsRepository.setPublishTime(hour, minute)
            container.workScheduler.reschedule(hour, minute, extra.value.settings.autoPublishEnabled)
        }
    }

    fun setSelectedCategories(categories: Set<String>) {
        viewModelScope.launch { container.settingsRepository.setSelectedCategories(categories) }
    }

    fun setCaptionTemplate(template: String) {
        viewModelScope.launch { container.settingsRepository.setCaptionTemplate(template) }
    }

    fun setAllowCommentsAllowDuet(allow: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAllowCommentsAllowDuet(allow) }
    }

    fun setMusicFolderUri(uri: String?) {
        viewModelScope.launch { container.settingsRepository.setMusicFolderUri(uri) }
    }

    private fun handleCallback(result: TikTokAuthManager.CallbackResult) {
        when (result) {
            is TikTokAuthManager.CallbackResult.Success -> {
                viewModelScope.launch {
                    val outcome = container.tikTokRepository.completeLogin(
                        code = result.code,
                        codeVerifier = result.codeVerifier,
                        redirectUri = container.authManager.redirectUri,
                    )
                    extra.value = if (outcome.isSuccess) {
                        extra.value.copy(isConnecting = false, isLinked = true, statusMessage = "Compte TikTok connecté !", isError = false)
                    } else {
                        extra.value.copy(isConnecting = false, statusMessage = outcome.exceptionOrNull()?.message, isError = true)
                    }
                }
            }
            is TikTokAuthManager.CallbackResult.Error -> {
                extra.value = extra.value.copy(isConnecting = false, statusMessage = result.message, isError = true)
            }
            TikTokAuthManager.CallbackResult.NotACallback -> Unit
        }
    }
}
