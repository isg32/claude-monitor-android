package com.example.data

import kotlinx.coroutines.flow.Flow

class UsageRepository(private val usageDao: UsageDao) {
    val allUsageLogs: Flow<List<UsageLog>> = usageDao.getAllUsageLogs()
    val apiConfig: Flow<ApiConfig?> = usageDao.getApiConfigFlow()

    suspend fun getDirectApiConfig(): ApiConfig? = usageDao.getApiConfig()

    suspend fun insertUsageLog(log: UsageLog) {
        usageDao.insertUsageLog(log)
    }

    suspend fun clearUsageLogs() {
        usageDao.clearUsageLogs()
    }

    suspend fun saveApiConfig(config: ApiConfig) {
        usageDao.saveApiConfig(config)
    }
}
