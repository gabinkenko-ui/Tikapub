package com.gabinkenko.tikapub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.gabinkenko.tikapub.worker.TikapubWorkerFactory

class TikapubApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(TikapubWorkerFactory(container))
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            PUBLISH_CHANNEL_ID,
            "Publication TikTok",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Progression de la génération et publication automatique des vidéos"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val PUBLISH_CHANNEL_ID = "tikapub_publish"
    }
}
