package com.gabinkenko.tikapub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.gabinkenko.tikapub.tiktok.AuthEventBus
import com.gabinkenko.tikapub.tiktok.TikTokAuthManager
import com.gabinkenko.tikapub.ui.TikapubNavHost
import com.gabinkenko.tikapub.ui.theme.TikapubTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as TikapubApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthRedirect(intent?.data)

        setContent {
            TikapubTheme {
                TikapubNavHost(container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthRedirect(intent.data)
    }

    private fun handleOAuthRedirect(uri: Uri?) {
        if (uri == null) return
        val result = container.authManager.parseCallback(uri)
        if (result !is TikTokAuthManager.CallbackResult.NotACallback) {
            lifecycleScope.launch { AuthEventBus.emit(result) }
        }
    }
}
