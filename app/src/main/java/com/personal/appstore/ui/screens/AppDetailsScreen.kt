package com.personal.appstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.appstore.domain.AppStatus
import com.personal.appstore.domain.model.AppRelease
import com.personal.appstore.domain.model.DownloadState
import com.personal.appstore.ui.AppItem
import com.personal.appstore.ui.Format
import com.personal.appstore.ui.components.AppIcon

/** Экран деталей: changelog, история версий, дата релиза. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    item: AppItem,
    onBack: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.app.name) },
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
            Header(item, onPrimaryAction)

            item.download.let { download ->
                when (download) {
                    is DownloadState.Downloading -> {
                        val fraction = download.fraction
                        if (fraction != null) {
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    is DownloadState.Failed -> Text(
                        text = download.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    else -> Unit
                }
            }

            Section(title = "Что нового") {
                Text(
                    text = item.app.latest.changelog.ifBlank { "Описание изменений не указано" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${item.app.latest.versionName} · ${Format.date(item.app.latest.releasedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Section(title = "Сведения") {
                InfoRow("Пакет", item.app.id)
                InfoRow("Версия", "${item.app.latest.versionName} (${item.app.latest.versionCode})")
                InfoRow("Размер", Format.bytes(item.app.latest.apkSizeBytes))
                InfoRow("Опубликовано", Format.date(item.app.latest.releasedAt))
                statusLine(item.status)?.let { InfoRow("Установлено", it) }
            }

            if (item.app.history.size > 1) {
                Section(title = "История версий") {
                    item.app.history.drop(1).forEach { release ->
                        HistoryRow(release)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(item: AppItem, onPrimaryAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(iconUrl = item.app.iconUrl, name = item.app.name, size = 72.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(item.app.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${item.app.latest.versionName} · ${Format.bytes(item.app.latest.apkSizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            item.download is DownloadState.Downloading || item.download is DownloadState.Verifying ->
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)

            item.download is DownloadState.Installing ->
                Text("Установка…", style = MaterialTheme.typography.labelLarge)

            item.download is DownloadState.Failed ->
                OutlinedButton(onClick = onPrimaryAction) { Text("Повторить") }

            item.status is AppStatus.NotInstalled ->
                Button(onClick = onPrimaryAction) { Text("Установить") }

            item.status is AppStatus.UpdateAvailable ->
                Button(onClick = onPrimaryAction) { Text("Обновить") }

            else -> FilledTonalButton(onClick = onPrimaryAction) { Text("Открыть") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HistoryRow(release: AppRelease) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "${release.versionName} (${release.versionCode}) · ${Format.date(release.releasedAt)}",
            style = MaterialTheme.typography.labelMedium,
        )
        if (release.changelog.isNotBlank()) {
            Text(
                text = release.changelog,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

private fun statusLine(status: AppStatus): String? = when (status) {
    is AppStatus.NotInstalled -> null
    is AppStatus.UpdateAvailable -> "${status.installed.versionName} (${status.installed.versionCode})"
    is AppStatus.UpToDate -> "${status.installed.versionName} — актуальная"
    is AppStatus.InstalledNewer -> "${status.installed.versionName} — новее витрины"
}
