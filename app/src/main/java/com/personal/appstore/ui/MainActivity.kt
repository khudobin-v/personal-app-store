package com.personal.appstore.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.appstore.StoreApplication
import com.personal.appstore.installer.InstallPermissions
import com.personal.appstore.ui.screens.AppDetailsScreen
import com.personal.appstore.ui.screens.AppListScreen
import com.personal.appstore.ui.screens.OnboardingScreen
import com.personal.appstore.ui.screens.SetupScreen
import com.personal.appstore.ui.theme.PersonalAppStoreTheme
import com.personal.appstore.worker.UpdateNotifier

/** Что показываем и насколько глубоко — от этого зависит направление анимации. */
private data class ScreenKey(
    val decided: Boolean,
    val onboarding: Boolean,
    val setup: Boolean,
    val detailsId: String?,
) {
    val depth: Int
        get() = when {
            !decided || onboarding -> 0
            setup || detailsId != null -> 2
            else -> 1
        }
}

class MainActivity : ComponentActivity() {

    private val viewModel: StoreViewModel by viewModels { StoreViewModel.Factory }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* отказ не критичен */ }

    private val unknownSourcesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (!InstallPermissions.canInstallPackages(this)) {
                Toast.makeText(
                    this,
                    "Без разрешения на установку магазин не сможет ставить приложения",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        // Магазин уже открыт — уведомление «Магазин обновлён» больше не нужно.
        UpdateNotifier(this).cancelSelfUpdated()

        setContent {
            PersonalAppStoreTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val setupState by viewModel.setupState.collectAsStateWithLifecycle()
                val onboarding by viewModel.onboarding.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
                var showSetup by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is StoreEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                            is StoreEvent.RequestUnknownSourcesPermission -> openUnknownSourcesSettings()
                            is StoreEvent.LaunchApp -> launchApp(event.packageName)
                        }
                    }
                }

                val selected = state.items.firstOrNull { it.id == selectedId }

                BackHandler(enabled = !onboarding.visible && (selected != null || showSetup)) {
                    if (showSetup) showSetup = false else selectedId = null
                }

                // Экран описывается ключом, а не текущими переменными: пока
                // уходящий экран доигрывает анимацию, он должен рисовать своё
                // прежнее содержимое, а не то, что уже выбрано.
                val screen = ScreenKey(
                    decided = onboarding.decided,
                    onboarding = onboarding.visible,
                    setup = showSetup,
                    detailsId = selectedId.takeIf { selected != null },
                )

                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val forward = targetState.depth >= initialState.depth
                        val shift = if (forward) 1 else -1
                        (
                            slideInHorizontally(tween(260)) { width -> shift * width / 6 } +
                                fadeIn(tween(220))
                            ) togetherWith (
                            slideOutHorizontally(tween(260)) { width -> -shift * width / 6 } +
                                fadeOut(tween(180))
                            )
                    },
                    label = "screen",
                ) { target ->
                    when {
                        !target.decided -> Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {}

                        target.onboarding -> OnboardingScreen(onStart = viewModel::completeOnboarding)

                        target.setup -> SetupScreen(
                            state = setupState,
                            onBack = { showSetup = false },
                            onAlwaysShowOnboardingChange = viewModel::setAlwaysShowOnboarding,
                            onLoginChange = viewModel::onLoginChange,
                            onSearch = viewModel::searchStorefronts,
                            onChoose = { url, login ->
                                viewModel.chooseStorefront(url, login)
                                showSetup = false
                            },
                            onManualUrlChange = viewModel::onManualUrlChange,
                            onApplyManualUrl = {
                                viewModel.applyManualUrl()
                                showSetup = false
                            },
                            onReset = viewModel::resetStorefront,
                        )

                        else -> {
                            val details = target.detailsId?.let { id -> state.items.firstOrNull { it.id == id } }
                            if (details != null) {
                                AppDetailsScreen(
                                    item = details,
                                    onBack = { selectedId = null },
                                    onPrimaryAction = { viewModel.onPrimaryAction(details) },
                                )
                            } else {
                                AppListScreen(
                                    state = state,
                                    snackbarHostState = snackbarHostState,
                                    onRefresh = viewModel::refresh,
                                    onPrimaryAction = viewModel::onPrimaryAction,
                                    onOpenDetails = { selectedId = it.id },
                                    onOpenSetup = { showSetup = true },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Пакеты могли измениться, пока нас не было (в том числе после установки).
        viewModel.refreshInstalled()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openUnknownSourcesSettings() {
        Toast.makeText(
            this,
            "Разрешите установку приложений из этого магазина",
            Toast.LENGTH_LONG,
        ).show()
        unknownSourcesLauncher.launch(InstallPermissions.unknownSourcesSettingsIntent(this))
    }

    private fun launchApp(packageName: String) {
        val locator = (application as StoreApplication).locator
        val intent: Intent? = locator.installedApps.launchIntent(packageName)
        if (intent == null) {
            Toast.makeText(this, "У приложения нет экрана запуска", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }
}
