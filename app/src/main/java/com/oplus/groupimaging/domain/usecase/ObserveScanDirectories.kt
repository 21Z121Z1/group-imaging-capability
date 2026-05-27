package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ObserveScanDirectories @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(): List<String> = repository.loadExtraScanDirectories()
}
