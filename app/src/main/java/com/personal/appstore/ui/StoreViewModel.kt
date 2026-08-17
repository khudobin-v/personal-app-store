package com.personal.appstore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.appstore.StoreApplication
import com.personal.appstore.StoreConfig
import com.personal.appstore.data.ManifestRepository
import com.personal.appstore.data.ManifestState
import com.personal.appstore.data.remote.ApkDownloader
import com.personal.appstore.data.remote.ChecksumMismatchException
import com.personal.appstore.domain.AppStatus
import com.personal.appstore.domain.AppStatusResolver
import com.personal.appstore.domain.InstalledAppsProvider
import com.personal.appstore.domain.model.DownloadState
import com.personal.appstore.domain.model.InstalledApp
import com.personal.appstore.domain.model.StoreApp
import com.personal.appstore.domain.needsDownload
import com.personal.appstore.installer.ApkInstaller
import com.personal.appstore.installer.InstallEvent
import com.personal.appstore.installer.InstallEvents
import com.personal.appstore.installer.InstallOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** Строка списка: приложение из витрины + его состояние на устройстве. */
data class AppItem(
    val app: StoreApp,
    val status: AppStatus,
    val download: DownloadState,
) {
    val id: String get() = app.id
    val isBusy: Boolean
        get() = download is DownloadState.Downloading ||
            download is DownloadState.Verifying ||
            download is DownloadState.Installing
}

data class StoreUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<AppItem> = emptyList(),
    /** Заполнено, если обновился сам магазин — показываем баннер. */
    val storeUpdate: AppItem? = null,
    val error: String? = null,
    val warning: String? = null,
    val isStale: Boolean = false,
    val lastUpdatedMillis: Long? = null,
)

/** Одноразовые события для Activity. */
sealed interface StoreEvent {
    data class ShowMessage(val text: String) : StoreEvent
    data object RequestUnknownSourcesPermission : StoreEvent
    data class LaunchApp(val packageName: String) : StoreEvent
}

class StoreViewModel(
    private val repository: ManifestRepository,
    private val installedApps: InstalledAppsProvider,
    private val downloader: ApkDownloader,
    private val installer: ApkInstaller,
    private val storePackageName: String = StoreConfig.storePackageName,
) : ViewModel() {

    private val downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    private val isRefreshing = MutableStateFlow(false)
    private val jobs = mutableMapOf<String, Job>()

    private val _events = Channel<StoreEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<StoreUiState> =
        combine(repository.state, installedApps.installed, downloads, isRefreshing, ::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoreUiState())

    init {
        viewModelScope.launch {
            repository.state.collect { state ->
                if (state is ManifestState.Ready) {
                    installedApps.refresh(state.manifest.apps.map { it.id })
                }
            }
        }
        viewModelScope.launch {
            InstallEvents.events.collect(::onInstallEvent)
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                repository.refresh()
            } finally {
                isRefreshing.value = false
            }
            refreshInstalled()
        }
    }

    /** Вызывается при возврате в приложение: пакеты могли измениться снаружи. */
    fun refreshInstalled() {
        val state = repository.state.value
        if (state is ManifestState.Ready) {
            installedApps.refresh(state.manifest.apps.map { it.id })
        }
    }

    fun onPrimaryAction(item: AppItem) {
        if (item.status.needsDownload) {
            downloadAndInstall(item.app)
        } else {
            _events.trySend(StoreEvent.LaunchApp(item.app.id))
        }
    }

    fun retryDownload(item: AppItem) {
        downloads.update { it - item.id }
        downloadAndInstall(item.app)
    }

    private fun downloadAndInstall(app: StoreApp) {
        if (jobs[app.id]?.isActive == true) return

        jobs[app.id] = viewModelScope.launch {
            setDownload(app.id, DownloadState.Downloading(0, app.latest.apkSizeBytes))
            try {
                val file = downloader.download(
                    appId = app.id,
                    release = app.latest,
                    onProgress = { downloaded, total ->
                        setDownload(app.id, DownloadState.Downloading(downloaded, total))
                    },
                    onVerifying = { setDownload(app.id, DownloadState.Verifying) },
                )

                when (val outcome = installer.install(file, app.id)) {
                    is InstallOutcome.Started -> setDownload(app.id, DownloadState.Installing)

                    is InstallOutcome.NeedsUnknownSourcesPermission -> {
                        setDownload(app.id, DownloadState.Idle)
                        _events.trySend(StoreEvent.RequestUnknownSourcesPermission)
                    }

                    is InstallOutcome.Failed -> {
                        setDownload(app.id, DownloadState.Failed(outcome.message))
                        _events.trySend(StoreEvent.ShowMessage("Не удалось установить: ${outcome.message}"))
                    }
                }
            } catch (e: CancellationException) {
                setDownload(app.id, DownloadState.Idle)
                throw e
            } catch (e: ChecksumMismatchException) {
                // Файл уже удалён загрузчиком — устанавливать нечего.
                setDownload(app.id, DownloadState.Failed("контрольная сумма не совпала"))
                _events.trySend(
                    StoreEvent.ShowMessage("APK ${app.name} повреждён или подменён — установка отменена"),
                )
            } catch (e: IOException) {
                setDownload(app.id, DownloadState.Failed(e.message ?: "ошибка сети"))
                _events.trySend(StoreEvent.ShowMessage("Не удалось скачать ${app.name}"))
            } catch (e: Exception) {
                setDownload(app.id, DownloadState.Failed(e.message ?: "неизвестная ошибка"))
            }
        }
    }

    private fun onInstallEvent(event: InstallEvent) {
        val packageName = event.packageName
        when (event) {
            is InstallEvent.Success -> {
                packageName?.let { setDownload(it, DownloadState.Idle) }
                refreshInstalled()
                viewModelScope.launch { cleanupCache() }
            }

            is InstallEvent.Cancelled -> packageName?.let { setDownload(it, DownloadState.Idle) }

            is InstallEvent.Failed -> {
                packageName?.let { setDownload(it, DownloadState.Failed(event.message)) }
                _events.trySend(StoreEvent.ShowMessage("Установка не удалась: ${event.message}"))
            }
        }
    }

    /** Оставляем в кэше только APK версий, которые сейчас в витрине. */
    private suspend fun cleanupCache() {
        val state = repository.state.value
        if (state !is ManifestState.Ready) return
        val keep = state.manifest.apps
            .filter { app ->
                val installed = installedApps.installed.value[app.id]
                AppStatusResolver.resolve(app.latest.versionCode, installed) !is AppStatus.UpToDate
            }
            .map { downloader.fileFor(it.id, it.latest) }
            .toSet()
        downloader.cleanup(keep)
    }

    private fun setDownload(appId: String, state: DownloadState) {
        downloads.update { current ->
            if (state is DownloadState.Idle) current - appId else current + (appId to state)
        }
    }

    private fun buildState(
        manifestState: ManifestState,
        installed: Map<String, InstalledApp>,
        downloads: Map<String, DownloadState>,
        refreshing: Boolean,
    ): StoreUiState = when (manifestState) {
        is ManifestState.Loading -> StoreUiState(isLoading = true, isRefreshing = refreshing)

        is ManifestState.Failed -> StoreUiState(
            isLoading = false,
            isRefreshing = refreshing,
            error = manifestState.message,
        )

        is ManifestState.Ready -> {
            val items = manifestState.manifest.apps.map { app ->
                AppItem(
                    app = app,
                    status = AppStatusResolver.resolve(app.latest.versionCode, installed[app.id]),
                    download = downloads[app.id] ?: DownloadState.Idle,
                )
            }
            StoreUiState(
                isLoading = false,
                isRefreshing = refreshing,
                items = items,
                storeUpdate = items.firstOrNull {
                    it.id == storePackageName && it.status is AppStatus.UpdateAvailable
                },
                warning = manifestState.warning,
                isStale = manifestState.isStale,
                lastUpdatedMillis = manifestState.fetchedAtMillis,
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StoreApplication
                StoreViewModel(
                    repository = app.locator.manifestRepository,
                    installedApps = app.locator.installedApps,
                    downloader = app.locator.apkDownloader,
                    installer = app.locator.apkInstaller,
                )
            }
        }
    }
}
