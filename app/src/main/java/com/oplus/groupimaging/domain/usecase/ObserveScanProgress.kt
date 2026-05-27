package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.ScanJob
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveScanProgress @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(): ScanJob? = repository.latestScanJob()

    fun flow(): Flow<ScanJob?> = repository.latestScanJobFlow()
}
