package com.oplus.groupimaging.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupImagingDatabaseMigrationTest {
    @Test
    fun migrate1To2_addsCursorTableAndAssetColumns() {
        openDatabase(TEST_DB, version = 1).use { database ->
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `media_assets` (
                    `assetId` TEXT NOT NULL,
                    `path` TEXT,
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
                    `parseStatus` TEXT NOT NULL,
                    `parseVersion` INTEGER NOT NULL,
                    `sourceConfidence` REAL NOT NULL,
                    `capturePairStatus` TEXT NOT NULL,
                    `contentSignature` TEXT NOT NULL,
                    PRIMARY KEY(`assetId`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `media_assets` (
                    `assetId`, `path`, `uri`, `fileName`, `mimeType`, `size`, `createdAt`, `modifiedAt`,
                    `isOplusOriginal`, `isLivePhoto`, `isRaw`, `pairedCaptureId`, `deviceModel`, `focalLength`,
                    `focalLengthEq`, `lensClass`, `captureModeLabel`, `userCommentRaw`, `parseStatus`,
                    `parseVersion`, `sourceConfidence`, `capturePairStatus`, `contentSignature`
                ) VALUES (
                    'asset-1', '/storage/emulated/0/DCIM/Camera/IMG_0001.JPG',
                    'content://media/external/images/media/1', 'IMG_0001.JPG', 'image/jpeg',
                    1024, 1000, 2000, 1, 0, 0, NULL, 'PKJ110', 5.58, 23, 'MAIN',
                    '夜景', 'oplus comment', 'PARSED', 1, 0.95, 'PRIMARY', 'sig-1'
                )
                """.trimIndent(),
            )
            createLegacySharedTables(database)

            GroupImagingDatabase.MIGRATION_1_2.migrate(database)

            database.query("SELECT mediaStoreId, relativePath, userCommentDigest FROM media_assets WHERE assetId = 'asset-1'")
                .use { cursor ->
                    check(cursor.moveToFirst()) { "expected migrated media asset row" }
                    check(cursor.getLong(0) == 0L) { "expected default mediaStoreId after migration" }
                    check(cursor.isNull(1)) { "expected relativePath to default to null" }
                    check(cursor.isNull(2)) { "expected userCommentDigest to default to null" }
                }
            database.query("PRAGMA table_info(`scan_cursors`)").use { cursor ->
                val names = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    names += cursor.getString(nameIndex)
                }
                check("rootsJson" in names)
                check("parseVersion" in names)
            }
            database.query("PRAGMA index_list(`media_assets`)").use { cursor ->
                var foundIndex = false
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "index_media_assets_mediaStoreId") {
                        foundIndex = true
                    }
                }
                check(foundIndex) { "expected index_media_assets_mediaStoreId to exist" }
            }
        }
    }

    @Test
    fun migrate2To3_addsScanDirectoriesTable() {
        openDatabase(TEST_DB_V2, version = 2).use { database ->
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `media_assets` (
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
                    PRIMARY KEY(`assetId`)
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_mediaStoreId` ON `media_assets` (`mediaStoreId`)")
            createLegacySharedTables(database)
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

            GroupImagingDatabase.MIGRATION_2_3.migrate(database)

            database.query("PRAGMA table_info(`scan_directories`)").use { cursor ->
                val names = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    names += cursor.getString(nameIndex)
                }
                check("relativePath" in names)
                check("updatedAt" in names)
            }
        }
    }

    @Test
    fun migrate3To4_addsGenerationCursorAndMovePlanTables() {
        openDatabase(TEST_DB_V3, version = 3).use { database ->
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `media_assets` (
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
                    PRIMARY KEY(`assetId`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `media_assets` (
                    `assetId`, `mediaStoreId`, `path`, `relativePath`, `uri`, `fileName`, `mimeType`,
                    `size`, `createdAt`, `modifiedAt`, `isOplusOriginal`, `isLivePhoto`, `isRaw`,
                    `pairedCaptureId`, `deviceModel`, `focalLength`, `focalLengthEq`, `lensClass`,
                    `captureModeLabel`, `userCommentRaw`, `userCommentDigest`, `parseStatus`,
                    `parseVersion`, `sourceConfidence`, `capturePairStatus`, `contentSignature`
                ) VALUES (
                    'asset-1', 1, NULL, 'DCIM/Camera/', 'content://media/external/file/1',
                    'IMG_0001.JPG', 'image/jpeg', 1024, 1000, 2000, 1, 0, 0, NULL,
                    'PKJ110', 5.58, 23, 'MAIN', '夜景', NULL, NULL, 'PARSED', 3, 1.0,
                    'PRIMARY', 'sig-1'
                )
                """.trimIndent(),
            )
            createLegacySharedTables(database)
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
                """
                INSERT INTO `scan_cursors` (
                    `scopeHash`, `rootsJson`, `parseVersion`, `lastSuccessfulModifiedAt`,
                    `lastSuccessfulMediaId`, `resumeModifiedAt`, `resumeMediaId`, `activeJobId`, `updatedAt`
                ) VALUES ('scope', '["DCIM/Camera"]', 3, 2000, 1, NULL, NULL, NULL, 3000)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `scan_directories` (
                    `relativePath` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`relativePath`)
                )
                """.trimIndent(),
            )

            GroupImagingDatabase.MIGRATION_3_4.migrate(database)

            database.query("SELECT generationId, assetId FROM media_assets WHERE assetId = 'asset-1'")
                .use { cursor ->
                    check(cursor.moveToFirst()) { "expected migrated media asset row" }
                    check(cursor.getLong(0) == 0L) { "expected legacy asset in generation 0" }
                    check(cursor.getString(1) == "asset-1")
                }
            database.query("SELECT activeGenerationId FROM index_generations WHERE generationKey = 'active'")
                .use { cursor ->
                    check(cursor.moveToFirst()) { "expected active generation row" }
                    check(cursor.getLong(0) == 0L)
                }
            database.query("SELECT lastScannedModifiedAt, lastScannedMediaId FROM scan_cursors WHERE scopeHash = 'scope'")
                .use { cursor ->
                    check(cursor.moveToFirst()) { "expected migrated cursor row" }
                    check(cursor.getLong(0) == 2000L)
                    check(cursor.getLong(1) == 1L)
                }
            database.query("PRAGMA table_info(`move_candidates`)").use { cursor ->
                val names = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    names += cursor.getString(nameIndex)
                }
                check("role" in names)
                check("status" in names)
                check("captureDate" in names)
            }
        }
    }

    private fun createLegacySharedTables(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `capture_sessions` (
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
                PRIMARY KEY(`captureId`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `device_profiles` (
                `deviceModel` TEXT NOT NULL,
                `aliasesJson` TEXT NOT NULL,
                `focalEqRangesJson` TEXT NOT NULL,
                `cameraIdMapJson` TEXT NOT NULL,
                `profileVersion` INTEGER NOT NULL,
                PRIMARY KEY(`deviceModel`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `smart_rules` (
                `ruleId` TEXT NOT NULL,
                `ruleName` TEXT NOT NULL,
                `conditionsJson` TEXT NOT NULL,
                `targetFolder` TEXT NOT NULL,
                `actionType` TEXT NOT NULL,
                `enabled` INTEGER NOT NULL,
                PRIMARY KEY(`ruleId`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scan_jobs` (
                `jobId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `totalCount` INTEGER NOT NULL,
                `scannedCount` INTEGER NOT NULL,
                `etaSeconds` INTEGER,
                `startedAt` INTEGER NOT NULL,
                `finishedAt` INTEGER,
                `parseVersion` INTEGER NOT NULL,
                `failureCount` INTEGER NOT NULL,
                PRIMARY KEY(`jobId`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `archive_audits` (
                `auditId` TEXT NOT NULL,
                `previewToken` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `movedCount` INTEGER NOT NULL,
                `failureCount` INTEGER NOT NULL,
                `failureJson` TEXT NOT NULL,
                PRIMARY KEY(`auditId`)
            )
            """.trimIndent(),
        )
    }

    private fun openDatabase(
        name: String,
        version: Int,
    ): SupportSQLiteDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        return helper.writableDatabase
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val TEST_DB_V2 = "migration-test-v2"
        const val TEST_DB_V3 = "migration-test-v3"
    }
}
