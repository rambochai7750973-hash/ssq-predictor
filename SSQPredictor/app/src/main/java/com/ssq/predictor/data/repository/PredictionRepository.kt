package com.ssq.predictor.data.repository

import com.ssq.predictor.data.local.dao.PredictionRecordDao
import com.ssq.predictor.data.local.entity.PredictionRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionRepository @Inject constructor(
    private val predictionRecordDao: PredictionRecordDao
) {
    private val maxBatches = 10

    fun getAllRecords(): Flow<List<PredictionRecordEntity>> = predictionRecordDao.getAllRecords()

    suspend fun getAllRecordsOnce(): List<PredictionRecordEntity> = predictionRecordDao.getAllRecordsOnce()

    suspend fun savePredictionBatch(records: List<PredictionRecordEntity>) {
        predictionRecordDao.insertAll(records)
        val batchCount = predictionRecordDao.getBatchCount()
        if (batchCount > maxBatches) {
            predictionRecordDao.deleteOldestBatches(batchCount - maxBatches)
        }
    }

    suspend fun deleteAll() = predictionRecordDao.deleteAll()
}
