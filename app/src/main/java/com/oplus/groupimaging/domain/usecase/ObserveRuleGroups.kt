package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ObserveRuleGroups @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(): List<RuleGroupSummary> = repository.loadRuleGroups()
}
