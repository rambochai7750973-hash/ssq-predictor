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
            warmUpCookies()
            return@withContext fetchFromApi(pageSize)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun warmUpCookies() {
        try {
            val refUrl = URL("https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/")
            val refConn = refUrl.openConnection() as HttpURLConnection
            refConn.requestMethod = "GET"
            refConn.connectTimeout = 8000
            refConn.readTimeout = 8000
            refConn.instanceFollowRedirects = true
            refConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            refConn.setRequestProperty("Referer", "https://www.cwl.gov.cn/")
            refConn.inputStream.read() // trigger the request
            refConn.disconnect()
        } catch (_: Exception) {
        }
    }

    private fun fetchFromApi(pageSize: Int): List<DrawEntity> {
        val apiUrl = URL("https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice?name=ssq&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd=&pageNo=1&pageSize=$pageSize&week=&systemType=PC")
        val connection = apiUrl.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        connection.setRequestProperty("Referer", "https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/")
        connection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01")
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            return parseDrawResponse(response)
        }
        connection.disconnect()
        return emptyList()
    }

    private fun parseDrawResponse(json: String): List<DrawEntity> {
        val draws = mutableListOf<DrawEntity>()
        try {
            val jsonObject = JSONObject(json)
            if (jsonObject.has("result")) {
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return draws
    }
}
