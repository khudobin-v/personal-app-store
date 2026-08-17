package com.personal.appstore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.appstore.domain.AppStatus
import com.personal.appstore.domain.model.DownloadState
import com.personal.appstore.ui.AppItem
import com.personal.appstore.ui.Format

/** Строка списка приложений: иконка, имя, версия, размер, статус. */
@Composable
fun AppRow(
    item: AppItem,
    onPrimaryAction: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(iconUrl = item.app.iconUrl, name = item.app.name)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            StatusButton(item = item, onClick = onPrimaryAction)
        }

        ProgressLine(item.download)
    }
}

@Composable
private fun StatusButton(item: AppItem, onClick: () -> Unit) {
    when (item.download) {
        is DownloadState.Downloading, is DownloadState.Verifying ->
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)

        is DownloadState.Installing ->
            Text(
                text = "Установка…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

        is DownloadState.Failed ->
            OutlinedButton(onClick = onClick) { Text("Повторить") }

        is DownloadState.Idle -> when (item.status) {
            is AppStatus.NotInstalled -> Button(onClick = onClick) { Text("Установить") }
            is AppStatus.UpdateAvailable -> Button(onClick = onClick) { Text("Обновить") }
            else -> FilledTonalButton(onClick = onClick) { Text("Открыть") }
        }
    }
}

@Composable
private fun ProgressLine(download: DownloadState) {
    when (download) {
        is DownloadState.Downloading -> {
            val fraction = download.fraction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "  ${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                }
                Text(
                    text = "  ${Format.bytes(download.downloadedBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is DownloadState.Verifying -> Text(
            text = "Проверка контрольной суммы…",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        is DownloadState.Failed -> Text(
            text = download.message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 6.dp),
        )

        else -> Unit
    }
}

private fun subtitle(item: AppItem): String {
    val size = Format.bytes(item.app.latest.apkSizeBytes)
    return when (val status = item.status) {
        is AppStatus.NotInstalled -> "${item.app.latest.versionName} · $size"
        is AppStatus.UpdateAvailable ->
            "${status.installed.versionName} → ${item.app.latest.versionName} · $size"

        is AppStatus.UpToDate -> "${item.app.latest.versionName} · установлено"
        is AppStatus.InstalledNewer ->
            "${status.installed.versionName} · новее витрины"
    }
}
