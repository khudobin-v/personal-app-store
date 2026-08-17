package com.personal.appstore.domain.model

/** Что сейчас происходит с приложением в списке. */
sealed interface DownloadState {

    data object Idle : DownloadState

    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : DownloadState {
        /** null, если сервер не сообщил размер и в манифесте его нет. */
        val fraction: Float?
            get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else null
    }

    /** Скачано, считаем sha256. */
    data object Verifying : DownloadState

    /** Файл проверен, управление передано системному установщику. */
    data object Installing : DownloadState

    data class Failed(val message: String) : DownloadState
}
