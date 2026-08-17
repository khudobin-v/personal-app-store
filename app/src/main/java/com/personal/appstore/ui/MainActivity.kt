package com.personal.appstore.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.appstore.StoreApplication
import com.personal.appstore.installer.InstallPermissions
import com.personal.appstore.ui.screens.AppDetailsScreen
import com.personal.appstore.ui.screens.AppListScreen
import com.personal.appstore.ui.screens.SetupScreen
import com.personal.appstore.ui.theme.PersonalAppStoreTheme

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

        setContent {
            PersonalAppStoreTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val setupState by viewModel.setupState.collectAsStateWithLifecycle()
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

                BackHandler(enabled = selected != null || showSetup) {
                    if (showSetup) showSetup = false else selectedId = null
                }

                if (showSetup) {
                    SetupScreen(
                        state = setupState,
                        onBack = { showSetup = false },
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
                } else if (selected != null) {
                    AppDetailsScreen(
                        item = selected,
                        onBack = { selectedId = null },
                        onPrimaryAction = { viewModel.onPrimaryAction(selected) },
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
