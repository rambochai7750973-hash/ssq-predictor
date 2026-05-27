package com.ssq.predictor.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar

data class CountdownData(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val label: String
)

fun calculateNextDraw(): CountdownData {
    val now = Calendar.getInstance()
    val beijingOffset = 8 * 60 * 60 * 1000L
    val nowMs = now.timeInMillis
    val beijingNow = Calendar.getInstance()
    beijingNow.timeInMillis = nowMs + beijingOffset

    val drawHour = 21
    val drawMinute = 15
    val drawDays = listOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SUNDAY)

    val currentDayOfWeek = beijingNow.get(Calendar.DAY_OF_WEEK)
    val currentHour = beijingNow.get(Calendar.HOUR_OF_DAY)
    val currentMinute = beijingNow.get(Calendar.MINUTE)

    var daysUntilDraw = Int.MAX_VALUE
    for (drawDay in drawDays) {
        var diff = drawDay - currentDayOfWeek
        if (diff < 0) diff += 7
        if (diff == 0 && (currentHour > drawHour || (currentHour == drawHour && currentMinute >= drawMinute))) {
            diff = 7
        }
        if (diff < daysUntilDraw) {
            daysUntilDraw = diff
        }
    }

    val nextDrawTime = Calendar.getInstance()
    nextDrawTime.timeInMillis = beijingNow.timeInMillis
    nextDrawTime.add(Calendar.DAY_OF_MONTH, daysUntilDraw)
    nextDrawTime.set(Calendar.HOUR_OF_DAY, drawHour)
    nextDrawTime.set(Calendar.MINUTE, drawMinute)
    nextDrawTime.set(Calendar.SECOND, 0)
    nextDrawTime.set(Calendar.MILLISECOND, 0)

    val diffMs = nextDrawTime.timeInMillis - beijingNow.timeInMillis
    val totalSeconds = maxOf(0, diffMs / 1000)
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val dayNames = mapOf(
        Calendar.TUESDAY to "周二",
        Calendar.THURSDAY to "周四",
        Calendar.SUNDAY to "周日"
    )
    val nextDayName = dayNames[nextDrawTime.get(Calendar.DAY_OF_WEEK)] ?: ""

    return CountdownData(
        days = days,
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        label = nextDayName
    )
}

@Composable
fun DrawCountdownCard() {
    var countdown by remember { mutableStateOf(calculateNextDraw()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            countdown = calculateNextDraw()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "下次开奖倒计时",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = countdown.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TimeUnit("天", countdown.days.toString())
                Spacer(modifier = Modifier.width(12.dp))
                TimeUnit("时", countdown.hours.toString())
                Spacer(modifier = Modifier.width(12.dp))
                TimeUnit("分", countdown.minutes.toString())
                Spacer(modifier = Modifier.width(12.dp))
                TimeUnit("秒", countdown.seconds.toString())
            }
        }
    }
}

@Composable
private fun TimeUnit(unit: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.padStart(2, '0'),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}
