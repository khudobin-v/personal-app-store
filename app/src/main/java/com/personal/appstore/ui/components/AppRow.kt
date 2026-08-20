package com.personal.appstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.appstore.domain.AppStatus
import com.personal.appstore.domain.model.DownloadState
import com.personal.appstore.ui.AppItem
import com.personal.appstore.ui.Format
import com.personal.appstore.ui.theme.tintFor

// Подложки карточек светлые всегда, поэтому текст на них тёмный в любой теме.
private val OnTint = Color(0xFF1B1B1F)
private val OnTintSoft = Color(0xFF5B5B66)

/**
 * Карточка приложения: пастельная подложка, крупная иконка, имя, автор и
 * плитки с версией и размером — как плитки коллекции в референсе.
 * Действие — «пилюля» справа: тёмная для установки, светлая для запуска.
 */
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(tintFor(item.app.id))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(iconUrl = item.app.iconUrl, name = item.app.name, size = 64.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.app.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = OnTint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.app.author?.let { author ->
                    Text(
                        text = "от $author",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnTintSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(text = item.app.latest.versionName)
                    Chip(text = Format.bytes(item.app.latest.apkSizeBytes))
                }
            }

            StatusAction(item = item, onClick = onPrimaryAction)
        }

        StatusLine(item)
    }
}

/** Полупрозрачная плитка поверх подложки — версия, размер. */
@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = OnTint,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.55f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
private fun StatusAction(item: AppItem, onClick: () -> Unit) {
    when (item.download) {
        is DownloadState.Downloading, is DownloadState.Verifying ->
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = OnTint)

        is DownloadState.Installing ->
            Text("Установка…", style = MaterialTheme.typography.labelMedium, color = OnTintSoft)

        is DownloadState.Failed -> PillButton(text = "Повторить", onClick = onClick)

        is DownloadState.Idle -> when (item.status) {
            is AppStatus.NotInstalled -> PillButton(text = "Установить", onClick = onClick)
            is AppStatus.UpdateAvailable -> PillButton(text = "Обновить", onClick = onClick)
            else -> PillButton(text = "Открыть", onClick = onClick, filled = false)
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit, filled: Boolean = true) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) OnTint else Color.White.copy(alpha = 0.75f),
            contentColor = if (filled) Color.White else OnTint,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatusLine(item: AppItem) {
    when (val download = item.download) {
        is DownloadState.Downloading -> {
            val fraction = download.fraction
            Box(modifier = Modifier.padding(top = 12.dp)) {
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = OnTint,
                        trackColor = Color.White.copy(alpha = 0.5f),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = OnTint,
                        trackColor = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }

        is DownloadState.Verifying -> Text(
            text = "Проверка контрольной суммы…",
            style = MaterialTheme.typography.labelSmall,
            color = OnTintSoft,
            modifier = Modifier.padding(top = 10.dp),
        )

        is DownloadState.Failed -> Text(
            text = download.message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 10.dp),
        )

        else -> {
            val status = item.status
            if (status is AppStatus.UpdateAvailable) {
                Text(
                    text = "стоит ${status.installed.versionName} · доступно ${item.app.latest.versionName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnTintSoft,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}
