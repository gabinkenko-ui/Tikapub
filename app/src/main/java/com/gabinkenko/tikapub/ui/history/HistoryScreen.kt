package com.gabinkenko.tikapub.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gabinkenko.tikapub.data.db.PublishLogEntity
import com.gabinkenko.tikapub.data.db.PublishStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val logs by viewModel.logs.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Historique des publications", style = MaterialTheme.typography.headlineSmall) }
        if (logs.isEmpty()) {
            item { Text("Rien pour l'instant.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(logs, key = { it.id }) { log -> HistoryRow(log) }
    }
}

@Composable
private fun HistoryRow(log: PublishLogEntity) {
    val (icon, tint) = when (log.status) {
        PublishStatus.SUCCESS -> Icons.Filled.CheckCircle to Color(0xFF00C853)
        PublishStatus.FAILED -> Icons.Filled.Error to MaterialTheme.colorScheme.error
        else -> Icons.Filled.HourglassEmpty to MaterialTheme.colorScheme.secondary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Column(Modifier.padding(start = 12.dp)) {
                Text(log.quoteText.ifBlank { "(vidéo sans citation)" }, style = MaterialTheme.typography.bodyMedium)
                Text(
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(log.timestampMs)),
                    style = MaterialTheme.typography.bodySmall,
                )
                log.tiktokPublishId?.let {
                    Text("publish_id: $it", style = MaterialTheme.typography.bodySmall)
                }
                log.message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
