package com.ssq.predictor.data.repository

import com.ssq.predictor.data.datasource.AssetDataSource
import com.ssq.predictor.data.local.dao.DrawDao
import com.ssq.predictor.data.local.entity.DrawEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawRepository @Inject constructor(
    private val drawDao: DrawDao,
    private val assetDataSource: AssetDataSource
) {
    fun getAllDraws(): Flow<List<DrawEntity>> = drawDao.getAllDraws()

    suspend fun getAllDrawsOnce(): List<DrawEntity> = drawDao.getAllDrawsOnce()

    suspend fun getDrawByPeriod(period: String): DrawEntity? = drawDao.getDrawByPeriod(period)

    suspend fun getRecentDraws(limit: Int): List<DrawEntity> = drawDao.getRecentDraws(limit)

    suspend fun getLatestDraw(): DrawEntity? = drawDao.getLatestDraw()

    suspend fun insertDraw(draw: DrawEntity) = drawDao.insert(draw)

    suspend fun deleteDraw(period: String) = drawDao.deleteByPeriod(period)

    suspend fun count(): Int = drawDao.count()

    suspend fun initializeIfEmpty() {
        if (count() == 0) {
            val history = assetDataSource.loadHistoryFromAsset()
            if (history.isNotEmpty()) {
                drawDao.insertAll(history)
            }
        }
    }
}
