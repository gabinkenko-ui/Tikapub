package com.gabinkenko.tikapub.ui.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.IconButton

@Composable
fun QuotesScreen(viewModel: QuotesViewModel) {
    val quotes by viewModel.quotes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une citation")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Citations (${quotes.size})", style = MaterialTheme.typography.headlineSmall)
                }
                items(quotes, key = { it.id }) { quote ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(quote.text, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    listOfNotNull(quote.author, quote.category).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(checked = quote.enabled, onCheckedChange = { viewModel.setEnabled(quote, it) })
                            IconButton(onClick = { viewModel.delete(quote) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddQuoteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text, author, category ->
                viewModel.addQuote(text, author, category)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddQuoteDialog(onDismiss: () -> Unit, onConfirm: (String, String?, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle citation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Texte") })
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Auteur (optionnel)") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text, author, category) }, enabled = text.isNotBlank()) {
                Text("Ajouter")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
