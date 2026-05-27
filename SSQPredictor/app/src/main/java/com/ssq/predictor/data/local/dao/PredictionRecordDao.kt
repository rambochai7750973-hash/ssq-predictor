package com.ssq.predictor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ssq.predictor.data.local.entity.PredictionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionRecordDao {

    @Query("SELECT * FROM prediction_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<PredictionRecordEntity>>

    @Query("SELECT * FROM prediction_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsOnce(): List<PredictionRecordEntity>

    @Query("SELECT DISTINCT batch_id FROM prediction_records ORDER BY batch_id DESC")
    suspend fun getAllBatchIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PredictionRecordEntity>)

    @Query("""
        DELETE FROM prediction_records 
        WHERE batch_id IN (
            SELECT batch_id FROM prediction_records 
            GROUP BY batch_id 
            ORDER BY batch_id ASC 
            LIMIT :count
        )
    """)
    suspend fun deleteOldestBatches(count: Int)

    @Query("SELECT COUNT(DISTINCT batch_id) FROM prediction_records")
    suspend fun getBatchCount(): Int

    @Query("DELETE FROM prediction_records")
    suspend fun deleteAll()
}
