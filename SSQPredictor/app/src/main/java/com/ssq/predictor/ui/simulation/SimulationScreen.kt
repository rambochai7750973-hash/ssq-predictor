package com.ssq.predictor.ui.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssq.predictor.domain.model.SimulationBatchResult
import com.ssq.predictor.domain.model.SimulationGroupResult
import com.ssq.predictor.domain.model.SimulationResult
import com.ssq.predictor.ui.components.BallView
import com.ssq.predictor.ui.components.ScoreBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimulationScreen(viewModel: SimulationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "模拟购买",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.isLoading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(32.dp)
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        } else if (!uiState.hasRecords) {
            item {
                Text(
                    text = "暂无预测记录，请先在预测页面生成预测",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val result = uiState.result ?: return@LazyColumn

            item {
                SummaryCard(result)
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "各期明细",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(result.batchResults) { batch ->
                BatchCard(batch)
            }
        }
    }
}

@Composable
private fun SummaryCard(result: SimulationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "模拟总览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("预测期数", "${result.totalBatches}", MaterialTheme.colorScheme.onPrimaryContainer)
                StatItem("总注数", "${result.totalGroups}", MaterialTheme.colorScheme.onPrimaryContainer)
                StatItem("中奖注数", "${result.winCount}", MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("总投入", "${result.totalCost}元", MaterialTheme.colorScheme.onPrimaryContainer)
                StatItem("总奖金", "${result.totalPrize}元", MaterialTheme.colorScheme.onPrimaryContainer)
                val profitColor = if (result.netProfit >= 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
                StatItem("净盈亏", "${result.netProfit}元", profitColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "中奖率: ${"%.1f".format(result.winRate * 100)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BatchCard(batch: SimulationBatchResult) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = batch.algorithmName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(batch.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "投入: ${batch.batchCost}元",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "奖金: ${batch.batchPrize}元",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (batch.batchPrize > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "中奖: ${batch.batchWinCount}/${batch.groups.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (batch.batchWinCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            batch.groups.forEachIndexed { index, group ->
                SimulationGroupRow(index = index + 1, group = group)
                if (index < batch.groups.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SimulationGroupRow(index: Int, group: SimulationGroupResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "#$index",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                items(group.reds) { red ->
                    BallView(number = red, isRed = true, size = 28.dp)
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            BallView(number = group.blue, isRed = false, size = 28.dp)
            Spacer(modifier = Modifier.width(4.dp))
            if (group.bestPrize != null) {
                ScoreBar(score = group.bestPrize.amount)
            }
        }
        if (group.bestPrize != null) {
            Text(
                text = "${group.bestPrize.label} - 奖金 ${group.bestPrize.amount}元 (${group.matches.firstOrNull()?.period ?: ""} ${group.matches.firstOrNull()?.date ?: ""})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
            )
        } else {
            Text(
                text = "未中奖",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
            )
        }
    }
}
