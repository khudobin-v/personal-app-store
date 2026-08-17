package com.personal.appstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.personal.appstore.ui.AppItem
import com.personal.appstore.ui.Format
import com.personal.appstore.ui.StoreUiState
import com.personal.appstore.ui.components.AppRow
import com.personal.appstore.ui.components.OfflineBanner
import com.personal.appstore.ui.components.StoreUpdateBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    state: StoreUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onPrimaryAction: (AppItem) -> Unit,
    onOpenDetails: (AppItem) -> Unit,
    onOpenSetup: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Мой магазин")
                        Text(
                            text = "обновлено ${Format.ago(state.lastUpdatedMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSetup) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки витрины")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> LoadingContent()
                state.error != null -> ErrorContent(state.error, onRefresh, onOpenSetup)
                else -> Content(state, onPrimaryAction, onOpenDetails)
            }
        }
    }
}

@Composable
private fun Content(
    state: StoreUiState,
    onPrimaryAction: (AppItem) -> Unit,
    onOpenDetails: (AppItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        state.storeUpdate?.let { storeItem ->
            item(key = "store-update") {
                StoreUpdateBanner(item = storeItem, onUpdate = { onPrimaryAction(storeItem) })
            }
        }

        if (state.isStale && state.warning != null) {
            item(key = "offline") {
                OfflineBanner(
                    message = state.warning,
                    lastUpdated = Format.ago(state.lastUpdatedMillis),
                )
            }
        }

        if (state.items.isEmpty()) {
            item(key = "empty") { EmptyContent() }
        }

        items(state.items, key = { it.id }) { item ->
            AppRow(
                item = item,
                onPrimaryAction = { onPrimaryAction(item) },
                onClick = { onOpenDetails(item) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Витрина пуста", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Сделайте первый релиз командой ./release.sh — приложение появится здесь автоматически.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onOpenSetup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Витрина недоступна", style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text("Повторить") }
        OutlinedButton(onClick = onOpenSetup) { Text("Настроить витрину") }
    }
}
