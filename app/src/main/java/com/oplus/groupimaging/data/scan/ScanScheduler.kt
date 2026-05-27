package com.oplus.groupimaging.data.scan

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.oplus.groupimaging.domain.ScanType
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray

@Singleton
class ScanScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun enqueue(scanType: ScanType, extraRoots: Set<String>) {
        val request = OneTimeWorkRequestBuilder<ScanWorker>()
            .setInputData(
                Data.Builder()
                    .putString(KEY_SCAN_TYPE, scanType.name)
                    .putString(KEY_EXTRA_ROOTS, JSONArray(extraRoots.sorted()).toString())
                    .build(),
            )
            .addTag(SCAN_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_SCAN_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

internal const val UNIQUE_SCAN_WORK_NAME = "group-imaging-scan"
internal const val SCAN_WORK_TAG = "group-imaging-scan"
internal const val KEY_SCAN_TYPE = "scanType"
internal const val KEY_EXTRA_ROOTS = "extraRoots"
