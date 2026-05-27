package com.oplus.groupimaging.testing

import com.oplus.groupimaging.navigation.AppRoute
import java.util.Locale

object TestTags {
    object Screen {
        const val HOME = "screen_home"
        const val CALENDAR = "screen_calendar"
        const val INSIGHT = "screen_insight"
        const val ALBUM_GROUPS = "screen_album_groups"
        const val SETTINGS = "screen_settings"
        const val SCAN_ONBOARDING = "screen_scan_onboarding"
        const val SCAN_PROGRESS = "screen_scan_progress"
        const val RULE_PREVIEW = "screen_rule_preview"
        const val FAILED_ITEMS = "screen_failed_items"
        const val DEVICE_PROFILES = "screen_device_profiles"
        const val DIRECTORY_MANAGER = "screen_directory_manager"
        const val SCOPE_EXPLANATION = "screen_scope_explanation"
    }

    object Navigation {
        const val HOME = "nav_home"
        const val CALENDAR = "nav_calendar"
        const val INSIGHT = "nav_insight"
        const val ALBUM_GROUPS = "nav_album_groups"
        const val SETTINGS = "nav_settings"

        fun forRoute(route: AppRoute): String = when (route) {
            AppRoute.Home -> HOME
            AppRoute.Calendar -> CALENDAR
            AppRoute.Insight -> INSIGHT
            AppRoute.AlbumGroups -> ALBUM_GROUPS
            AppRoute.Settings -> SETTINGS
            else -> "nav_${normalize(route.route)}"
        }
    }

    object PermissionDialog {
        const val ROOT = "permission_dialog_root"
        const val CONFIRM = "permission_dialog_confirm"
        const val DISMISS = "permission_dialog_dismiss"
    }

    object Home {
        const val EMPTY_STATE = "home_empty_state"
        const val START_SCAN = "home_start_scan"
        const val TOTAL_CARD = "home_total_card"
        const val MAIN_LENS = "home_lens_main"
        const val ULTRA_WIDE_LENS = "home_lens_ultra_wide"
        const val TELE_LENS = "home_lens_tele"
        const val LIVE_CARD = "home_live_card"
        const val RAW_CARD = "home_raw_card"
        const val INCREMENTAL_SCAN = "home_incremental_scan"
        const val SCAN_STATUS = "home_scan_status"
    }

    object Calendar {
        const val VIEW_DAY_STATS = "calendar_view_day_stats"
        fun mode(modeName: String): String = "calendar_mode_${normalize(modeName)}"
    }

    object Insight {
        const val OPEN_FILTER = "insight_open_filter"
        const val FILTER_BOTTOM_SHEET = "insight_filter_bottom_sheet"
        const val FILTER_APPLY = "insight_filter_apply"
    }

    object AlbumGroups {
        fun ruleCard(ruleId: String): String = "album_rule_${normalize(ruleId)}"
    }

    object RulePreview {
        const val MOVE_ALL = "rule_preview_move_all"
    }

    object Settings {
        const val MANAGE_DIRECTORIES = "settings_manage_directories"
        const val RUN_FULL_SCAN = "settings_run_full_scan"
        const val PARSER_VERSION = "settings_parser_version"
        const val DEVICE_PROFILE_COUNT = "settings_device_profile_count"
        const val SCOPE_EXPLANATION = "settings_scope_explanation"
    }

    object DirectoryManager {
        const val INPUT = "directory_manager_input"
        const val ADD = "directory_manager_add"
    }

    object ScanOnboarding {
        const val START_SCAN = "scan_onboarding_start_scan"
    }

    object ScanProgress {
        const val HERO = "scan_progress_hero"
        const val VIEW_FAILED = "scan_progress_view_failed"
        const val FINISH = "scan_progress_finish"
    }

    object Dialogs {
        const val MOVE_CONFIRM = "move_confirm_dialog"
        const val MOVE_CONFIRM_CONTINUE = "move_confirm_continue"
        const val MOVE_CONFIRM_CANCEL = "move_confirm_cancel"
        const val MOVE_PROGRESS = "move_progress_dialog"
        const val MOVE_PROGRESS_CONFIRM = "move_progress_confirm"
    }
}

private fun normalize(value: String): String {
    val asciiSlug = value
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    if (asciiSlug.isNotEmpty()) {
        return asciiSlug
    }
    return value
        .map { it.code.toString(16) }
        .joinToString("_")
}
