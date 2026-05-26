package com.ssq.predictor.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssq.predictor.ui.components.BallView

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "历史走势",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.draws) { draw ->
                HistoryCard(draw)
            }
        }
    }
}

@Composable
private fun HistoryCard(draw: com.ssq.predictor.data.local.entity.DrawEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${draw.period} 期",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = draw.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reds = listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(reds) { red ->
                        BallView(number = red, isRed = true, size = 32.dp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                BallView(number = draw.blue, isRed = false, size = 32.dp)
            }

            val sum = listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6).sum() + draw.blue
            val span = listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6).let {
                it.max() - it.min()
            }
            val odd = listOf(draw.red1, draw.red2, draw.red3, draw.red4, draw.red5, draw.red6).count { it % 2 == 1 }

            Text(
                text = "和值: $sum  跨度: $span  奇偶: $odd:${6 - odd}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
