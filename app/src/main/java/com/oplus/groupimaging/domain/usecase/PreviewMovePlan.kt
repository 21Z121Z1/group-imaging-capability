package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.FilterSpec
import com.oplus.groupimaging.domain.MovePreview
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class PreviewMovePlan @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(ruleName: String, targetFolder: String, filters: FilterSpec): MovePreview =
        repository.previewMove(ruleName, targetFolder, filters)
}
