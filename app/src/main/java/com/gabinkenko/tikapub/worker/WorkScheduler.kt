package com.gabinkenko.tikapub.worker

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

/** Schedules the daily [PublishWorker] run and exposes a manual "publish now" trigger. */
class WorkScheduler(private val context: Context) {

    private val workManager get() = WorkManager.getInstance(context)

    fun reschedule(hour: Int, minute: Int, enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<PublishWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints())
            .addTag(PublishWorker.TAG_PERIODIC)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<PublishWorker>()
            .addTag(PublishWorker.TAG_MANUAL)
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniqueWork(UNIQUE_MANUAL_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun observeManualRun(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_MANUAL_WORK).asFlow()

    fun observePeriodicRun(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_PERIODIC_WORK).asFlow()

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK = "tikapub_periodic_publish"
        const val UNIQUE_MANUAL_WORK = "tikapub_manual_publish"
    }
}
