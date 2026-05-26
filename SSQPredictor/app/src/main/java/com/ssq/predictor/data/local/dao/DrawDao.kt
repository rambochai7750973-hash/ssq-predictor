package com.ssq.predictor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ssq.predictor.data.local.entity.DrawEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {

    @Query("SELECT * FROM history_draws ORDER BY period DESC")
    fun getAllDraws(): Flow<List<DrawEntity>>

    @Query("SELECT * FROM history_draws ORDER BY period DESC")
    suspend fun getAllDrawsOnce(): List<DrawEntity>

    @Query("SELECT * FROM history_draws WHERE period = :period")
    suspend fun getDrawByPeriod(period: String): DrawEntity?

    @Query("SELECT * FROM history_draws ORDER BY period DESC LIMIT :limit")
    suspend fun getRecentDraws(limit: Int): List<DrawEntity>

    @Query("SELECT * FROM history_draws ORDER BY period DESC LIMIT 1")
    suspend fun getLatestDraw(): DrawEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(draws: List<DrawEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draw: DrawEntity)

    @Query("DELETE FROM history_draws WHERE period = :period")
    suspend fun deleteByPeriod(period: String)

    @Query("SELECT COUNT(*) FROM history_draws")
    suspend fun count(): Int
}
