package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC")
    fun getAllUsageLogs(): Flow<List<UsageLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLog(log: UsageLog)

    @Query("DELETE FROM usage_logs")
    suspend fun clearUsageLogs()

    @Query("SELECT * FROM api_configs WHERE id = 1 LIMIT 1")
    fun getApiConfigFlow(): Flow<ApiConfig?>

    @Query("SELECT * FROM api_configs WHERE id = 1 LIMIT 1")
    suspend fun getApiConfig(): ApiConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveApiConfig(config: ApiConfig)
}
