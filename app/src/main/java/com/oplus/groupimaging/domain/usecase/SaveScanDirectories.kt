package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class SaveScanDirectories @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(directories: List<String>): List<String> =
        repository.saveExtraScanDirectories(directories)
}
