package com.gabinkenko.tikapub

import android.content.Context
import com.gabinkenko.tikapub.data.db.AppDatabase
import com.gabinkenko.tikapub.data.settings.AppSettingsRepository
import com.gabinkenko.tikapub.data.settings.SecureTokenStore
import com.gabinkenko.tikapub.tiktok.TikTokAuthManager
import com.gabinkenko.tikapub.tiktok.TikTokNetwork
import com.gabinkenko.tikapub.tiktok.TikTokRepository
import com.gabinkenko.tikapub.video.VideoComposerEngine
import com.gabinkenko.tikapub.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-rolled dependency container (no DI framework) shared by the UI, workers and repositories. */
class AppContainer(private val context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(context, applicationScope) }
    val quoteDao by lazy { database.quoteDao() }
    val publishLogDao by lazy { database.publishLogDao() }

    val settingsRepository by lazy { AppSettingsRepository(context) }
    val tokenStore by lazy { SecureTokenStore(context) }

    val authManager by lazy { TikTokAuthManager(context, tokenStore) }

    private val apiHttpClient by lazy { TikTokNetwork.okHttpClient(debug = BuildConfig.DEBUG) }
    private val uploadHttpClient by lazy { TikTokNetwork.uploadHttpClient() }
    private val apiService by lazy { TikTokNetwork.apiService(apiHttpClient) }
    val tikTokRepository by lazy { TikTokRepository(apiService, uploadHttpClient, tokenStore) }

    val videoComposerEngine by lazy { VideoComposerEngine(context) }

    val workScheduler by lazy { WorkScheduler(context) }
}
