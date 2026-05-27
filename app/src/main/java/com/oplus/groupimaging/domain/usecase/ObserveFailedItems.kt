package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.FailedItem
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ObserveFailedItems @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(): List<FailedItem> = repository.loadFailedItems()
}
