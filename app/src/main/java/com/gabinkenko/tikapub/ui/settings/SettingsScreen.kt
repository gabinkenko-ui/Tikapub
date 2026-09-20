package com.gabinkenko.tikapub.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gabinkenko.tikapub.data.settings.TikTokPrivacyLevel
import com.gabinkenko.tikapub.tiktok.TikTokAuthManager

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, authManager: TikTokAuthManager) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val musicFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setMusicFolderUri(uri.toString())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { Text("Réglages", style = MaterialTheme.typography.headlineMedium) }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compte TikTok Developer", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Crée une app sur developers.tiktok.com et colle ici sa Client Key / Client Secret. Voir README.md pour la procédure complète.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = state.clientKey,
                        onValueChange = viewModel::onClientKeyChanged,
                        label = { Text("Client Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.clientSecret,
                        onValueChange = viewModel::onClientSecretChanged,
                        label = { Text("Client Secret") },
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.redirectUri,
                        onValueChange = viewModel::onRedirectUriChanged,
                        label = { Text("Redirect URI (page GitHub Pages)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isLinked) {
                            OutlinedButton(onClick = viewModel::disconnect) { Text("Déconnecter") }
                        } else {
                            Button(
                                onClick = {
                                    if (viewModel.startLogin()) {
                                        authManager.launchLogin()
                                    }
                                },
                            ) { Text(if (state.isConnecting) "Connexion..." else "Se connecter à TikTok") }
                        }
                    }

                    state.statusMessage?.let {
                        Text(
                            it,
                            color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        item { HorizontalDivider() }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confidentialité des publications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Sans audit TikTok validé, seul \"Privé\" fonctionnera de façon fiable pour les nouvelles apps. Voir README.md.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TikTokPrivacyLevel.entries.forEach { level ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.settings.privacyLevel == level,
                                onClick = { viewModel.setPrivacyLevel(level) },
                            )
                            Text(level.label)
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Heure de publication quotidienne", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.settings.publishHour.toString(),
                            onValueChange = { v ->
                                v.toIntOrNull()?.coerceIn(0, 23)?.let { viewModel.setPublishTime(it, state.settings.publishMinute) }
                            },
                            label = { Text("Heure") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.settings.publishMinute.toString(),
                            onValueChange = { v ->
                                v.toIntOrNull()?.coerceIn(0, 59)?.let { viewModel.setPublishTime(state.settings.publishHour, it) }
                            },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catégories de citations utilisées", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Aucune sélection = toutes les catégories.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.categories.forEach { category ->
                            val selected = category in state.settings.selectedCategories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val updated = state.settings.selectedCategories.toMutableSet()
                                    if (selected) updated.remove(category) else updated.add(category)
                                    viewModel.setSelectedCategories(updated)
                                },
                                label = { Text(category) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Musique de fond", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choisis un dossier contenant tes fichiers audio libres de droits (mp3/m4a/wav). Une piste est tirée au hasard à chaque vidéo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        state.settings.musicFolderUri?.let { "Dossier configuré ✓" } ?: "Aucun dossier configuré (vidéos sans musique)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = { musicFolderLauncher.launch(null) }) {
                        Text("Choisir un dossier")
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Légende de publication", style = MaterialTheme.typography.titleMedium)
                    Text("{quote} sera remplacé par le texte de la citation.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = state.settings.captionTemplate,
                        onValueChange = viewModel::setCaptionTemplate,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Autoriser commentaires / duo / stitch", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = state.settings.allowCommentsAllowDuet,
                        onCheckedChange = viewModel::setAllowCommentsAllowDuet,
                    )
                }
            }
        }
    }
}
