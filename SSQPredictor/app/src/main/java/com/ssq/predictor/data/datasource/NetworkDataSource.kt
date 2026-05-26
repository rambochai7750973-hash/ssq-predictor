package com.ssq.predictor.data.datasource

import com.ssq.predictor.data.local.entity.DrawEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDataSource @Inject constructor() {

    suspend fun fetchLatestDraws(pageSize: Int = 30): List<DrawEntity> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&pageNo=1&pageSize=$pageSize&systemType=PC")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                return@withContext parseDrawResponse(response)
            }
            connection.disconnect()
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseDrawResponse(json: String): List<DrawEntity> {
        val draws = mutableListOf<DrawEntity>()
        try {
            val jsonObject = JSONObject(json)
            val result = jsonObject.getJSONArray("result")
            for (i in 0 until result.length()) {
                val item = result.getJSONObject(i)
                val code = item.getString("code")
                val date = item.getString("date")
                val red = item.getString("red")
                val blue = item.getString("blue")

                val reds = red.split(",").map { it.trim().toInt() }
                if (reds.size == 6) {
                    draws.add(
                        DrawEntity(
                            period = code,
                            date = date,
                            red1 = reds[0],
                            red2 = reds[1],
                            red3 = reds[2],
                            red4 = reds[3],
                            red5 = reds[4],
                            red6 = reds[5],
                            blue = blue.trim().toInt()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return draws
    }
}
