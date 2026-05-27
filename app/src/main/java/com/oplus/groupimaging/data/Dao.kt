package com.oplus.groupimaging.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaAssetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaAssetEntity)

    @Query("DELETE FROM media_assets WHERE generationId = :generationId")
    suspend fun clearGeneration(generationId: Long)

    @Query("DELETE FROM media_assets")
    suspend fun clearAll()

    @Query(
        """
        SELECT * FROM media_assets
        WHERE generationId = COALESCE(
            (SELECT activeGenerationId FROM index_generations WHERE generationKey = 'active'),
            0
        )
        ORDER BY createdAt DESC
        """,
    )
    suspend fun getAll(): List<MediaAssetEntity>

    @Query("SELECT * FROM media_assets WHERE generationId = :generationId ORDER BY createdAt DESC")
    suspend fun getAllForGeneration(generationId: Long): List<MediaAssetEntity>

    @Query(
        """
        SELECT * FROM media_assets
        WHERE mediaStoreId IN (:mediaStoreIds)
            AND generationId = COALESCE(
                (SELECT activeGenerationId FROM index_generations WHERE generationKey = 'active'),
                0
            )
        """,
    )
    suspend fun getByMediaStoreIds(mediaStoreIds: List<Long>): List<MediaAssetEntity>
}

@Dao
interface CaptureSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CaptureSessionEntity>)

    @Query("DELETE FROM capture_sessions WHERE generationId = :generationId")
    suspend fun clearGeneration(generationId: Long)

    @Query("DELETE FROM capture_sessions")
    suspend fun clearAll()

    @Query(
        """
        SELECT * FROM capture_sessions
        WHERE generationId = COALESCE(
            (SELECT activeGenerationId FROM index_generations WHERE generationKey = 'active'),
            0
        )
        ORDER BY captureTime DESC
        """,
    )
    suspend fun getAll(): List<CaptureSessionEntity>

    @Query("SELECT * FROM capture_sessions WHERE generationId = :generationId ORDER BY captureTime DESC")
    suspend fun getAllForGeneration(generationId: Long): List<CaptureSessionEntity>
}

@Dao
interface DeviceProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DeviceProfileEntity>)

    @Query("SELECT * FROM device_profiles")
    suspend fun getAll(): List<DeviceProfileEntity>
}

@Dao
interface SmartRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: SmartRuleEntity)

    @Query("SELECT * FROM smart_rules WHERE enabled = 1 ORDER BY ruleName")
    suspend fun getEnabled(): List<SmartRuleEntity>
}

@Dao
interface ScanJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: ScanJobEntity)

    @Query("SELECT * FROM scan_jobs ORDER BY startedAt DESC LIMIT 1")
    suspend fun latest(): ScanJobEntity?

    @Query("SELECT * FROM scan_jobs ORDER BY startedAt DESC LIMIT 1")
    fun latestFlow(): Flow<ScanJobEntity?>
}

@Dao
interface ScanDirectoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ScanDirectoryEntity>)

    @Query("DELETE FROM scan_directories")
    suspend fun clearAll()

    @Query("SELECT * FROM scan_directories ORDER BY relativePath COLLATE NOCASE ASC")
    suspend fun getAll(): List<ScanDirectoryEntity>
}

@Dao
interface ScanCursorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cursor: ScanCursorEntity)

    @Query("SELECT * FROM scan_cursors WHERE scopeHash = :scopeHash LIMIT 1")
    suspend fun get(scopeHash: String): ScanCursorEntity?
}

@Dao
interface IndexGenerationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: IndexGenerationEntity)

    @Query("SELECT activeGenerationId FROM index_generations WHERE generationKey = 'active' LIMIT 1")
    suspend fun activeGenerationId(): Long?
}

@Dao
interface MovePlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: MovePlanEntity)

    @Query("SELECT * FROM move_plans WHERE token = :token LIMIT 1")
    suspend fun get(token: String): MovePlanEntity?
}

@Dao
interface MoveCandidateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MoveCandidateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MoveCandidateEntity)

    @Query("SELECT * FROM move_candidates WHERE planToken = :token ORDER BY role, fileName")
    suspend fun getForPlan(token: String): List<MoveCandidateEntity>
}

@Dao
interface ArchiveAuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ArchiveAuditEntity)

    @Query("SELECT * FROM archive_audits ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ArchiveAuditEntity>
}
