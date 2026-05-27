package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.ScanType
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ScheduleScan @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(
        scanType: ScanType = ScanType.FULL,
        extraRoots: Set<String> = emptySet(),
    ) = repository.scheduleScan(scanType, extraRoots)
}
