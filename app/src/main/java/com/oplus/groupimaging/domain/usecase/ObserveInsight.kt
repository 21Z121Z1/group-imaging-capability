package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.InsightDimension
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ObserveInsight @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(
        dimension: InsightDimension,
        filters: FilterSpec = FilterSpec(),
    ): List<InsightBucket> = repository.loadInsightBreakdown(dimension, filters)
}
