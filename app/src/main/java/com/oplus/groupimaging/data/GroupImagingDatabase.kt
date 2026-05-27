package com.oplus.groupimaging.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaAssetEntity::class,
        CaptureSessionEntity::class,
        DeviceProfileEntity::class,
        SmartRuleEntity::class,
        ScanJobEntity::class,
        ScanDirectoryEntity::class,
        ScanCursorEntity::class,
        IndexGenerationEntity::class,
        MovePlanEntity::class,
        MoveCandidateEntity::class,
        ArchiveAuditEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class GroupImagingDatabase : RoomDatabase() {
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun captureSessionDao(): CaptureSessionDao
    abstract fun deviceProfileDao(): DeviceProfileDao
    abstract fun smartRuleDao(): SmartRuleDao
    abstract fun scanJobDao(): ScanJobDao
    abstract fun scanDirectoryDao(): ScanDirectoryDao
    abstract fun scanCursorDao(): ScanCursorDao
    abstract fun indexGenerationDao(): IndexGenerationDao
    abstract fun movePlanDao(): MovePlanDao
    abstract fun moveCandidateDao(): MoveCandidateDao
    abstract fun archiveAuditDao(): ArchiveAuditDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_assets ADD COLUMN mediaStoreId INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE media_assets ADD COLUMN relativePath TEXT")
                database.execSQL("ALTER TABLE media_assets ADD COLUMN userCommentDigest TEXT")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scan_cursors` (
                        `scopeHash` TEXT NOT NULL,
                        `rootsJson` TEXT NOT NULL,
                        `parseVersion` INTEGER NOT NULL,
                        `lastSuccessfulModifiedAt` INTEGER,
                        `lastSuccessfulMediaId` INTEGER,
                        `resumeModifiedAt` INTEGER,
                        `resumeMediaId` INTEGER,
                        `activeJobId` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`scopeHash`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_assets_mediaStoreId` ON `media_assets` (`mediaStoreId`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scan_directories` (
                        `relativePath` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`relativePath`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `index_generations` (
                        `generationKey` TEXT NOT NULL,
                        `activeGenerationId` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`generationKey`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `index_generations` (`generationKey`, `activeGenerationId`, `updatedAt`)
                    VALUES ('active', 0, 0)
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `media_assets_new` (
                        `generationId` INTEGER NOT NULL,
                        `assetId` TEXT NOT NULL,
                        `mediaStoreId` INTEGER NOT NULL,
                        `path` TEXT,
                        `relativePath` TEXT,
                        `uri` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `size` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `modifiedAt` INTEGER NOT NULL,
                        `isOplusOriginal` INTEGER NOT NULL,
                        `isLivePhoto` INTEGER NOT NULL,
                        `isRaw` INTEGER NOT NULL,
                        `pairedCaptureId` TEXT,
                        `deviceModel` TEXT,
                        `focalLength` REAL,
                        `focalLengthEq` INTEGER,
                        `lensClass` TEXT NOT NULL,
                        `captureModeLabel` TEXT,
                        `userCommentRaw` TEXT,
                        `userCommentDigest` TEXT,
                        `parseStatus` TEXT NOT NULL,
                        `parseVersion` INTEGER NOT NULL,
                        `sourceConfidence` REAL NOT NULL,
                        `capturePairStatus` TEXT NOT NULL,
                        `contentSignature` TEXT NOT NULL,
                        PRIMARY KEY(`generationId`, `assetId`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO `media_assets_new` (
                        `generationId`, `assetId`, `mediaStoreId`, `path`, `relativePath`, `uri`, `fileName`,
                        `mimeType`, `size`, `createdAt`, `modifiedAt`, `isOplusOriginal`, `isLivePhoto`,
                        `isRaw`, `pairedCaptureId`, `deviceModel`, `focalLength`, `focalLengthEq`,
                        `lensClass`, `captureModeLabel`, `userCommentRaw`, `userCommentDigest`,
                        `parseStatus`, `parseVersion`, `sourceConfidence`, `capturePairStatus`, `contentSignature`
                    )
                    SELECT
                        0, `assetId`, `mediaStoreId`, `path`, `relativePath`, `uri`, `fileName`,
                        `mimeType`, `size`, `createdAt`, `modifiedAt`, `isOplusOriginal`, `isLivePhoto`,
                        `isRaw`, `pairedCaptureId`, `deviceModel`, `focalLength`, `focalLengthEq`,
                        `lensClass`, `captureModeLabel`, `userCommentRaw`, `userCommentDigest`,
                        `parseStatus`, `parseVersion`, `sourceConfidence`, `capturePairStatus`, `contentSignature`
                    FROM `media_assets`
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE `media_assets`")
                database.execSQL("ALTER TABLE `media_assets_new` RENAME TO `media_assets`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId` ON `media_assets` (`generationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId_mediaStoreId` ON `media_assets` (`generationId`, `mediaStoreId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId_captureModeLabel` ON `media_assets` (`generationId`, `captureModeLabel`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId_deviceModel` ON `media_assets` (`generationId`, `deviceModel`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId_focalLengthEq` ON `media_assets` (`generationId`, `focalLengthEq`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId_lensClass` ON `media_assets` (`generationId`, `lensClass`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId_createdAt` ON `media_assets` (`generationId`, `createdAt`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_generationId_modifiedAt` ON `media_assets` (`generationId`, `modifiedAt`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `capture_sessions_new` (
                        `generationId` INTEGER NOT NULL,
                        `captureId` TEXT NOT NULL,
                        `primaryAssetId` TEXT NOT NULL,
                        `pairedRawAssetId` TEXT,
                        `captureTime` INTEGER NOT NULL,
                        `deviceModel` TEXT,
                        `lensClass` TEXT NOT NULL,
                        `focalLengthEq` INTEGER,
                        `isLivePhoto` INTEGER NOT NULL,
                        `isRawCapture` INTEGER NOT NULL,
                        `captureModeLabel` TEXT,
                        PRIMARY KEY(`generationId`, `captureId`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO `capture_sessions_new` (
                        `generationId`, `captureId`, `primaryAssetId`, `pairedRawAssetId`, `captureTime`,
                        `deviceModel`, `lensClass`, `focalLengthEq`, `isLivePhoto`, `isRawCapture`, `captureModeLabel`
                    )
                    SELECT
                        0, `captureId`, `primaryAssetId`, `pairedRawAssetId`, `captureTime`,
                        `deviceModel`, `lensClass`, `focalLengthEq`, `isLivePhoto`, `isRawCapture`, `captureModeLabel`
                    FROM `capture_sessions`
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE `capture_sessions`")
                database.execSQL("ALTER TABLE `capture_sessions_new` RENAME TO `capture_sessions`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_capture_sessions_generationId` ON `capture_sessions` (`generationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_capture_sessions_generationId_captureTime` ON `capture_sessions` (`generationId`, `captureTime`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_capture_sessions_generationId_lensClass` ON `capture_sessions` (`generationId`, `lensClass`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_capture_sessions_generationId_deviceModel` ON `capture_sessions` (`generationId`, `deviceModel`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_capture_sessions_generationId_focalLengthEq` ON `capture_sessions` (`generationId`, `focalLengthEq`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_capture_sessions_generationId_captureModeLabel` ON `capture_sessions` (`generationId`, `captureModeLabel`)")

                database.execSQL("ALTER TABLE `scan_cursors` ADD COLUMN `lastScannedModifiedAt` INTEGER")
                database.execSQL("ALTER TABLE `scan_cursors` ADD COLUMN `lastScannedMediaId` INTEGER")
                database.execSQL("UPDATE `scan_cursors` SET `lastScannedModifiedAt` = `lastSuccessfulModifiedAt`, `lastScannedMediaId` = `lastSuccessfulMediaId`")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `move_plans` (
                        `token` TEXT NOT NULL,
                        `ruleName` TEXT NOT NULL,
                        `targetFolder` TEXT NOT NULL,
                        `targetRelativePath` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `completedAt` INTEGER,
                        PRIMARY KEY(`token`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `move_candidates` (
                        `planToken` TEXT NOT NULL,
                        `assetId` TEXT NOT NULL,
                        `sourcePath` TEXT,
                        `sourceUri` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `destinationFileName` TEXT NOT NULL,
                        `destinationRelativePath` TEXT NOT NULL,
                        `destinationPath` TEXT NOT NULL,
                        `conflict` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `failureReason` TEXT,
                        `captureDate` TEXT,
                        `deviceModel` TEXT,
                        `focalLengthEq` INTEGER,
                        `captureModeLabel` TEXT,
                        `isLivePhoto` INTEGER NOT NULL,
                        `isRaw` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`planToken`, `assetId`, `role`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_move_candidates_planToken` ON `move_candidates` (`planToken`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_move_candidates_status` ON `move_candidates` (`status`)")
            }
        }

        fun build(context: Context): GroupImagingDatabase =
            Room.databaseBuilder(context, GroupImagingDatabase::class.java, "group_imaging.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
