package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.HomeSummary
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ObserveHomeSummary @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(filters: FilterSpec = FilterSpec()): HomeSummary = repository.loadHomeSummary(filters)
}
