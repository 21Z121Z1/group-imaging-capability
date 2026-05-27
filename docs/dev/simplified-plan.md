# Simplified Version Plan

## Goal
Keep only two core features:
1. **Scan gallery** — scan device photos and build index
2. **Move to My Albums** — move photos to DCIM/myalbums/ by rules

## Modules to REMOVE

| Module | File | Notes |
|--------|------|-------|
| Home | ui/home/HomeFeature.kt | Summary, stats |
| Calendar | ui/calendar/CalendarFeature.kt | Calendar view |
| Insight | ui/insight/InsightFeature.kt | Multi-dim analytics, filters |
| Failed Items | ui/failed/FailedItemsFeature.kt | Failed items list |
| Device Profiles | ui/deviceprofiles/DeviceProfilesFeature.kt | Device config viewer |
| Settings subpages | ui/settings/SettingsSubpages.kt | ScopeExplanation etc |

## Files to KEEP

### Core scanning engine
- core/MediaScanner.kt, MetadataExtractor.kt, OplusCommentDecoder.kt, OplusClassifier.kt, CaptureSessionMatcher.kt

### Data layer
- data/GroupImagingDatabase.kt, Entities.kt, Dao.kt, GroupImagingRepository.kt
- data/scan/ScanScheduler.kt, ScanWorker.kt, ScanDirectoryConfig.kt

### Domain use cases (keep these 7)
- ScheduleScan, ObserveScanProgress, ObserveScanDirectories, SaveScanDirectories, ObserveRuleGroups, PreviewMovePlan, ExecuteMovePlan

### UI (keep these 5)
- ui/scan/onboarding/ScanOnboardingFeature.kt
- ui/scan/progress/ScanProgressFeature.kt
- ui/album/groups/AlbumGroupsFeature.kt
- ui/album/preview/RulePreviewFeature.kt
- ui/settings/SettingsFeature.kt (simplified)

### Navigation & infra
- navigation/AppRoute.kt, AppRoot.kt, RootChrome.kt
- di/AppModule.kt

## Steps

### Step 1: Create branch (DONE: release/simplified)

### Step 2: Modify navigation
1. **AppRoute.kt** — remove Home, Calendar, Insight, FailedItems, DeviceProfiles, ScopeExplanation routes. Keep AlbumGroups, Settings, ScanOnboarding, ScanProgress, RulePreview, DirectoryManager.
2. **RootChrome.kt** — remove Home/Calendar/Insight from topLevelRouteDefinitions. Only keep AlbumGroups and Settings in bottom nav.
3. **AppRoot.kt** — remove filterSheetRequest state (Insight filter). Remove Home/Calendar/Insight/FailedItems/DeviceProfiles/ScopeExplanation composables. Change startDestination to AlbumGroups. Simplify GlobalSheetsAndDialogs.

### Step 3: Simplify Settings
1. **SettingsFeature.kt** — remove ObserveDeviceProfiles dependency. Remove OnOpenDeviceProfiles, OnOpenFailedItems, OnOpenScopeExplanation actions. Remove device profiles card, parse version card, data scope card. Keep scan directories and scan & update sections.

### Step 4: Remove unused UseCases
Delete:
- domain/usecase/ObserveHomeSummary.kt
- domain/usecase/ObserveCalendarData.kt
- domain/usecase/ObserveInsight.kt
- domain/usecase/ObserveFailedItems.kt
- domain/usecase/ObserveDeviceProfiles.kt

### Step 5: Remove unused UI modules
Delete:
- ui/home/ directory
- ui/calendar/ directory
- ui/insight/ directory
- ui/failed/ directory
- ui/deviceprofiles/ directory
- ui/settings/SettingsSubpages.kt

### Step 6: Clean up imports and dependencies
- Remove all imports referencing deleted modules
- Ensure compilation passes

### Step 7: Database
- KEEP device_profiles and archive_audits tables (do not remove)
- No schema changes needed

## Additional requirements
1. Keep "rescan" functionality
2. Clean up unused icons
3. Update tests to match removed modules
4. MUST NOT modify build.gradle.kts, AndroidManifest.xml, or resource XML files

## Flow after simplification
1. First launch → ScanOnboarding → ScanProgress → AlbumGroups
2. Incremental scan → Settings → ScanProgress
3. Browse albums → AlbumGroups → RulePreview → Move confirm → MoveProgress
4. Manage directories → Settings → DirectoryManager
