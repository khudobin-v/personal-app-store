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

/**
 * Карточка приложения: подложка в тон иконке, имя, автор и плитки с версией и
 * размером. Действие — «пилюля» справа: заливная для установки, лёгкая для
 * запуска.
 */
@Composable
fun AppRow(
    item: AppItem,
    onPrimaryAction: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    // Половина основного цвета иконки. Иконки может не быть — тогда остаётся
    // пастельная подложка по packageName, как раньше.
    val accent = rememberIconAccent(item.app.iconUrl, fallback = tintFor(item.app.id))
    val tint = accent.asBannerTint(background)
    val ink = tint.readableInk()
    val inkSoft = ink.copy(alpha = 0.72f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(tint)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(iconUrl = item.app.iconUrl, name = item.app.name, size = 56.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.app.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                item.app.author?.let { author ->
                    Text(
                        text = "от $author",
                        style = MaterialTheme.typography.labelSmall,
                        color = inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${item.app.latest.versionName} · ${Format.bytes(item.app.latest.apkSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            StatusAction(item = item, onClick = onPrimaryAction, ink = ink, inkSoft = inkSoft)
        }

        StatusLine(item = item, inkSoft = inkSoft, ink = ink)
    }
}

@Composable
private fun StatusAction(
    item: AppItem,
    onClick: () -> Unit,
    ink: Color,
    inkSoft: Color,
) {
    when (item.download) {
        is DownloadState.Downloading, is DownloadState.Verifying ->
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = ink)

        is DownloadState.Installing ->
            Text("Установка…", style = MaterialTheme.typography.labelSmall, color = inkSoft)

        is DownloadState.Failed -> PillButton("Повторить", onClick, ink)

        is DownloadState.Idle -> when (item.status) {
            is AppStatus.NotInstalled -> PillButton("Установить", onClick, ink)
            is AppStatus.UpdateAvailable -> PillButton("Обновить", onClick, ink)
            else -> PillButton("Открыть", onClick, ink, filled = false)
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    ink: Color,
    filled: Boolean = true,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            // Цвет текста берём контрастом к самой заливке, а не цветом
            // карточки — иначе надпись уходит в тон подложки.
            containerColor = if (filled) ink else ink.copy(alpha = 0.14f),
            contentColor = if (filled) ink.readableInk() else ink,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatusLine(item: AppItem, inkSoft: Color, ink: Color) {
    when (val download = item.download) {
        is DownloadState.Downloading -> {
            val fraction = download.fraction
            Box(modifier = Modifier.padding(top = 12.dp)) {
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = ink,
                        trackColor = ink.copy(alpha = 0.2f),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = ink,
                        trackColor = ink.copy(alpha = 0.2f),
                    )
                }
            }
        }

        is DownloadState.Verifying -> Text(
            text = "Проверка контрольной суммы…",
            style = MaterialTheme.typography.labelSmall,
            color = inkSoft,
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
                    color = inkSoft,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}
