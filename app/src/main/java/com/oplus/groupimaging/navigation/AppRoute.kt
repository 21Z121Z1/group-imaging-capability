package com.oplus.groupimaging.navigation

import com.oplus.groupimaging.domain.ScanType

sealed class AppRoute(val route: String, val label: String? = null, val isTopLevel: Boolean = false) {
    data object Home : AppRoute("home", "首页", true)
    data object Calendar : AppRoute("calendar", "日历", true)
    data object Insight : AppRoute("insight", "洞察", true)
    data object AlbumGroups : AppRoute("album_groups", "相册", true)
    data object Settings : AppRoute("settings", "设置", true)

    data object ScanOnboarding : AppRoute("scan_onboarding")
    data object ScanProgress : AppRoute("scan_progress?scanType={scanType}") {
        fun create(scanType: ScanType?) = if (scanType == null) "scan_progress" else "scan_progress?scanType=${scanType.name}"
    }
    data object RulePreview : AppRoute("rule_preview/{ruleId}") {
        fun create(ruleId: String) = "rule_preview/$ruleId"
    }
    data object FailedItems : AppRoute("failed_items")
    data object DeviceProfiles : AppRoute("device_profiles")
    data object DirectoryManager : AppRoute("directory_manager")
    data object ScopeExplanation : AppRoute("scope_explanation")
}
