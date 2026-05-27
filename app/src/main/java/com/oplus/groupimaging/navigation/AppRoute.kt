package com.oplus.groupimaging.navigation

import com.oplus.groupimaging.domain.ScanType

sealed class AppRoute(val route: String, val label: String? = null, val isTopLevel: Boolean = false) {
    data object AlbumGroups : AppRoute("album_groups", "相册", true)
    data object Settings : AppRoute("settings", "设置", true)

    data object ScanOnboarding : AppRoute("scan_onboarding")
    data object ScanProgress : AppRoute("scan_progress?scanType={scanType}") {
        fun create(scanType: ScanType?) = if (scanType == null) "scan_progress" else "scan_progress?scanType=${scanType.name}"
    }
    data object RulePreview : AppRoute("rule_preview/{ruleId}") {
        fun create(ruleId: String) = "rule_preview/$ruleId"
    }
    data object DirectoryManager : AppRoute("directory_manager")
}
