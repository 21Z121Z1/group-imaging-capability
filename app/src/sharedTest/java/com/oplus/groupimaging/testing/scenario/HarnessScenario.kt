package com.oplus.groupimaging.testing

import com.oplus.groupimaging.domain.MoveExecutionOutcome
import com.oplus.groupimaging.domain.MoveResult

class HarnessScenario private constructor(
    private val configureBlock: FakeInsightRepository.() -> Unit,
) {
    fun applyTo(repository: FakeInsightRepository) {
        repository.reset()
        repository.configureBlock()
    }

    class Builder internal constructor(
        private var seedIndexedLibrary: Boolean,
    ) {
        private var extraDirectories: List<String> = emptyList()
        private var refreshSeedsIndexedLibrary: Boolean = false
        private var moveResultOverride: MoveResult? = null
        private var moveExecutionOutcomeOverride: MoveExecutionOutcome? = null

        fun withExtraDirectories(vararg directories: String) = apply {
            extraDirectories = directories.toList()
        }

        fun refreshCompletesWithIndexedLibrary() = apply {
            refreshSeedsIndexedLibrary = true
        }

        fun withMoveResult(moveResult: MoveResult) = apply {
            moveResultOverride = moveResult
            moveExecutionOutcomeOverride = MoveExecutionOutcome.Completed(moveResult)
        }

        fun withMoveExecutionOutcome(outcome: MoveExecutionOutcome) = apply {
            moveExecutionOutcomeOverride = outcome
        }

        fun build(): HarnessScenario = HarnessScenario {
            if (seedIndexedLibrary) {
                seedIndexedLibrary()
            } else {
                seedEmptyLibrary()
            }
            extraScanDirectories = extraDirectories
            if (refreshSeedsIndexedLibrary) {
                onRefreshSuccess = { seedIndexedLibrary() }
            }
            moveResultOverride?.let { moveResult = it }
            moveExecutionOutcomeOverride?.let { moveExecutionOutcome = it }
        }
    }

    companion object {
        fun emptyLibrary(builder: Builder.() -> Unit = {}): HarnessScenario =
            Builder(seedIndexedLibrary = false).apply(builder).build()

        fun indexedLibrary(builder: Builder.() -> Unit = {}): HarnessScenario =
            Builder(seedIndexedLibrary = true).apply(builder).build()
    }
}
