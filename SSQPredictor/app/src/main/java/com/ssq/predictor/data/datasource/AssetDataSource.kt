package com.ssq.predictor.data.datasource

import android.content.Context
import com.ssq.predictor.data.local.entity.DrawEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun loadHistoryFromAsset(): List<DrawEntity> {
        val draws = mutableListOf<DrawEntity>()
        try {
            val inputStream = context.assets.open("ssq_history.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 8) {
                    draws.add(
                        DrawEntity(
                            period = parts[0].trim(),
                            date = parts[1].trim(),
                            red1 = parts[2].trim().toInt(),
                            red2 = parts[3].trim().toInt(),
                            red3 = parts[4].trim().toInt(),
                            red4 = parts[5].trim().toInt(),
                            red5 = parts[6].trim().toInt(),
                            red6 = parts[7].trim().toInt(),
                            blue = parts[8].trim().toInt()
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return draws
    }
}
