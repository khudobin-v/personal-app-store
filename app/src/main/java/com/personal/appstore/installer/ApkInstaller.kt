package com.personal.appstore.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/** Чем закончился запуск установки. Сам результат установки приходит в [InstallEvents]. */
sealed interface InstallOutcome {

    /** Установщик запущен, дальше решает пользователь/система. */
    data object Started : InstallOutcome

    /** Нужно разрешение «Установка неизвестных приложений» — UI ведёт в настройки. */
    data object NeedsUnknownSourcesPermission : InstallOutcome

    data class Failed(val message: String) : InstallOutcome
}

interface ApkInstaller {
    /** [file] обязан быть уже проверенным по sha256. */
    suspend fun install(file: File, appId: String): InstallOutcome
}

/** Результат установки от системы. */
sealed interface InstallEvent {
    val packageName: String?

    data class Success(override val packageName: String?) : InstallEvent
    data class Cancelled(override val packageName: String?) : InstallEvent
    data class Failed(override val packageName: String?, val message: String) : InstallEvent
}

/** Шина результатов PackageInstaller: receiver → ViewModel. */
object InstallEvents {

    private val _events = MutableSharedFlow<InstallEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<InstallEvent> = _events.asSharedFlow()

    fun publish(event: InstallEvent) {
        _events.tryEmit(event)
    }
}

/** Разрешение на установку из этого приложения. */
object InstallPermissions {

    fun canInstallPackages(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Экран «Установка неизвестных приложений» именно для нашего пакета. */
    fun unknownSourcesSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )
}
