package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.MoveExecutionOutcome
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ExecuteMovePlan @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(
        previewToken: String,
        writeAccessGranted: Boolean = false,
    ): MoveExecutionOutcome = repository.executeMove(previewToken, writeAccessGranted)
}
