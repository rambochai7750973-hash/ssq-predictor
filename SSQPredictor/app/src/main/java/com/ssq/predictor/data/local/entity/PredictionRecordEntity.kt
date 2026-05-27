package com.ssq.predictor.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prediction_records",
    indices = [Index(value = ["batch_id"])]
)
data class PredictionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "batch_id")
    val batchId: Long,
    @ColumnInfo(name = "algorithm_name")
    val algorithmName: String,
    @ColumnInfo(name = "red1") val red1: Int,
    @ColumnInfo(name = "red2") val red2: Int,
    @ColumnInfo(name = "red3") val red3: Int,
    @ColumnInfo(name = "red4") val red4: Int,
    @ColumnInfo(name = "red5") val red5: Int,
    @ColumnInfo(name = "red6") val red6: Int,
    @ColumnInfo(name = "blue") val blue: Int,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
