package com.gabinkenko.tikapub.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gabinkenko.tikapub.data.db.PublishStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshLinkState() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Tikapub", style = MaterialTheme.typography.headlineMedium)
            Text("@gabinkenko", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            ConnectionCard(isLinked = state.isLinked)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Publication automatique", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tous les jours à %02d:%02d".format(state.settings.publishHour, state.settings.publishMinute),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = state.settings.autoPublishEnabled,
                            onCheckedChange = { viewModel.setAutoPublishEnabled(it) },
                            enabled = state.isLinked,
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.publishNow() },
                enabled = state.isLinked && !state.isPublishing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Publication en cours...")
                } else {
                    Text("Publier maintenant")
                }
            }
        }

        item {
            Text("Dernière publication", style = MaterialTheme.typography.titleMedium)
        }

        val lastLog = state.lastLog
        if (lastLog == null) {
            item { Text("Aucune publication pour le moment.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (lastLog.status == PublishStatus.SUCCESS) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (lastLog.status == PublishStatus.SUCCESS) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(lastLog.quoteText.take(80), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(lastLog.timestampMs)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            lastLog.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(isLinked: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isLinked) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (isLinked) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    if (isLinked) "Compte TikTok connecté" else "Compte TikTok non connecté",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!isLinked) {
                    Text(
                        "Rends-toi dans Réglages pour te connecter.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
