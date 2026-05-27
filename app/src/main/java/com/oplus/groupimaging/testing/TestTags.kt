package com.oplus.groupimaging.testing

import com.oplus.groupimaging.navigation.AppRoute
import java.util.Locale

object TestTags {
    object Screen {
        const val ALBUM_GROUPS = "screen_album_groups"
        const val SETTINGS = "screen_settings"
        const val SCAN_ONBOARDING = "screen_scan_onboarding"
        const val SCAN_PROGRESS = "screen_scan_progress"
        const val RULE_PREVIEW = "screen_rule_preview"
        const val DIRECTORY_MANAGER = "screen_directory_manager"
    }

    object Navigation {
        const val ALBUM_GROUPS = "nav_album_groups"
        const val SETTINGS = "nav_settings"

        fun forRoute(route: AppRoute): String = when (route) {
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

    object AlbumGroups {
        fun ruleCard(ruleId: String): String = "album_rule_${normalize(ruleId)}"
    }

    object RulePreview {
        const val MOVE_ALL = "rule_preview_move_all"
    }

    object Settings {
        const val MANAGE_DIRECTORIES = "settings_manage_directories"
        const val RUN_FULL_SCAN = "settings_run_full_scan"
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
