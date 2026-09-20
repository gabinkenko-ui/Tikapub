package com.gabinkenko.tikapub.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.gabinkenko.tikapub.AppContainer

class TikapubWorkerFactory(private val container: AppContainer) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        PublishWorker::class.java.name -> PublishWorker(appContext, workerParameters, container)
        else -> null
    }
}
