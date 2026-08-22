package com.personal.appstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.appstore.domain.model.DownloadState
import com.personal.appstore.ui.AppItem

/**
 * Шапка списка: приложение, которое обновилось последним. Подложка — половина
 * основного цвета его иконки, поэтому баннер каждый раз выглядит «в тон»
 * обновившемуся приложению.
 */
@Composable
fun LatestUpdateBanner(
    item: AppItem,
    onClick: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    val accent = rememberIconAccent(item.app.iconUrl, fallback = MaterialTheme.colorScheme.secondaryContainer)
    // Цвет иконки приходит асинхронно — переливаем подложку, а не переключаем рывком.
    val tint by animateColorAsState(
        targetValue = accent.asBannerTint(background),
        animationSpec = tween(durationMillis = 350),
        label = "tint",
    )
    val ink = tint.readableInk()
    val inkSoft = ink.copy(alpha = 0.82f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(tint)
            .clickable(onClick = onClick),
    ) {
        // Подложка уходит под строку статуса, отступ держит только содержимое.
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Последнее обновление",
                    style = MaterialTheme.typography.labelSmall,
                    color = inkSoft,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSetup) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Настройки",
                        tint = ink,
                    )
                }
            }

            Row(
                modifier = Modifier.padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.app.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.app.author?.takeIf { it.isNotBlank() }?.let { author ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = inkSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = inkSoft,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = "Версия ${item.app.latest.versionName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = inkSoft,
                    )
                }
                AppIcon(
                    iconUrl = item.app.iconUrl,
                    name = item.app.name,
                    size = 96.dp,
                )
            }

            item.app.latest.changelog.takeIf { it.isNotBlank() }?.let { changelog ->
                Column(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Что нового:",
                        style = MaterialTheme.typography.labelLarge,
                        color = ink,
                    )
                    Text(
                        text = changelog.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = inkSoft,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** «Обновить магазин»: сам магазин публикуется в витрине как обычное приложение. */
@Composable
fun StoreUpdateBanner(
    item: AppItem,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Доступно обновление магазина",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Версия ${item.app.latest.versionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (item.isBusy) {
                Text(
                    text = when (item.download) {
                        is DownloadState.Installing -> "Установка…"
                        is DownloadState.Verifying -> "Проверка…"
                        else -> "Скачивание…"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Button(onClick = onUpdate) { Text("Обновить") }
            }
        }
    }
}

/** Данные из кэша: сеть недоступна или витрина не ответила. */
@Composable
fun OfflineBanner(
    message: String,
    lastUpdated: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Показан сохранённый список, обновлён $lastUpdated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
