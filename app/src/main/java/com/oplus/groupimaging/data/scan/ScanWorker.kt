package com.oplus.groupimaging.data.scan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val repository: OplusInsightRepository,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        val scanType = inputData.getString(KEY_SCAN_TYPE)
            ?.let { runCatching { ScanType.valueOf(it) }.getOrNull() }
            ?: ScanType.FULL
        val extraRoots = inputData.getString(KEY_EXTRA_ROOTS)
            ?.let(::decodeExtraRoots)
            ?: emptySet()

        return runCatching {
            repository.refreshLibrary(scanType, extraRoots)
            Result.success()
        }.getOrElse {
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        ensureNotificationChannel()
        val cancelIntent = WorkManager.getInstance(appContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("正在扫描摄影库")
            .setContentText("离开页面后扫描会继续运行")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "摄影库扫描",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun decodeExtraRoots(value: String): Set<String> {
        val json = JSONArray(value)
        return buildSet {
            for (index in 0 until json.length()) {
                add(json.getString(index))
            }
        }
    }

    private companion object {
        const val CHANNEL_ID = "group_imaging_scan"
        const val NOTIFICATION_ID = 4107
    }
}
