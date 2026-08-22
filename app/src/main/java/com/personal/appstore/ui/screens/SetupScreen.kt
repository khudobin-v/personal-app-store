package com.personal.appstore.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.appstore.ui.SetupState

/**
 * Настройка витрины: вводим GitHub-логин, магазин сам находит репозиторий с
 * apps.json среди публичных репозиториев. Ручной ввод адреса остаётся на
 * случай нестандартного размещения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    state: SetupState,
    onBack: () -> Unit,
    onAlwaysShowOnboardingChange: (Boolean) -> Unit,
    onLoginChange: (String) -> Unit,
    onSearch: () -> Unit,
    onChoose: (url: String, login: String) -> Unit,
    onManualUrlChange: (String) -> Unit,
    onApplyManualUrl: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Сейчас используется", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = state.currentUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (state.isCustom) "выбрано на устройстве" else "значение из сборки",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.isCustom) {
                        TextButton(onClick = onReset) { Text("Вернуть значение из сборки") }
                    }
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Найти по GitHub-логину", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Магазин просмотрит публичные репозитории и возьмёт тот, где лежит apps.json.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = state.login,
                        onValueChange = onLoginChange,
                        label = { Text("GitHub-логин") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = onSearch, enabled = !state.isSearching) { Text("Найти") }
                        if (state.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    state.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    state.results.forEach { found ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChoose(found.manifestUrl, state.login.trim()) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(found.repository, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "приложений в витрине: ${found.appCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            OutlinedButton(onClick = { onChoose(found.manifestUrl, state.login.trim()) }) {
                                Text("Выбрать")
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Или указать адрес вручную", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.manualUrl,
                        onValueChange = onManualUrlChange,
                        label = { Text("https://raw.githubusercontent.com/…/apps.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onApplyManualUrl() }),
                    )
                    Button(onClick = onApplyManualUrl) { Text("Сохранить") }
                }
            }

            DeveloperModeCard(
                alwaysShowOnboarding = state.alwaysShowOnboarding,
                onAlwaysShowOnboardingChange = onAlwaysShowOnboardingChange,
            )
        }
    }
}

/** Раздел для отладки оформления: сюда складываем переключатели «для себя». */
@Composable
private fun DeveloperModeCard(
    alwaysShowOnboarding: Boolean,
    onAlwaysShowOnboardingChange: (Boolean) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Режим разработчика", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlwaysShowOnboardingChange(!alwaysShowOnboarding) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Показывать onboarding при каждом запуске",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Иначе приветственный экран появляется только при первом запуске.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = alwaysShowOnboarding,
                    onCheckedChange = onAlwaysShowOnboardingChange,
                )
            }
        }
    }
}
