package com.personal.appstore.domain

import com.personal.appstore.domain.model.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStatusResolverTest {

    private fun installed(versionCode: Long, versionName: String = "1.0.0") =
        InstalledApp("com.example.app", versionCode, versionName)

    @Test
    fun `нет пакета — предлагаем установить`() {
        assertEquals(AppStatus.NotInstalled, AppStatusResolver.resolve(5, null))
    }

    @Test
    fun `установлена старая версия — предлагаем обновить`() {
        val status = AppStatusResolver.resolve(latestVersionCode = 5, installed = installed(4))
        assertTrue(status is AppStatus.UpdateAvailable)
        assertEquals(4L, (status as AppStatus.UpdateAvailable).installed.versionCode)
    }

    @Test
    fun `версии совпали — предлагаем открыть`() {
        assertTrue(AppStatusResolver.resolve(5, installed(5)) is AppStatus.UpToDate)
    }

    @Test
    fun `установлено новее витрины — не предлагаем откат`() {
        val status = AppStatusResolver.resolve(5, installed(6))
        assertTrue(status is AppStatus.InstalledNewer)
        assertFalse(status.needsDownload)
    }

    @Test
    fun `сравнение идёт по versionCode, а не по versionName`() {
        // versionName «9.0.0» лексикографически больше «10.0.0», versionCode решает.
        val status = AppStatusResolver.resolve(
            latestVersionCode = 10,
            installed = installed(versionCode = 9, versionName = "9.0.0"),
        )
        assertTrue(status is AppStatus.UpdateAvailable)
    }

    @Test
    fun `скачивать нужно только для установки и обновления`() {
        assertTrue(AppStatusResolver.resolve(5, null).needsDownload)
        assertTrue(AppStatusResolver.resolve(5, installed(4)).needsDownload)
        assertFalse(AppStatusResolver.resolve(5, installed(5)).needsDownload)
    }

    @Test
    fun `запускать можно всё, что установлено`() {
        assertFalse(AppStatusResolver.resolve(5, null).isInstalled)
        assertTrue(AppStatusResolver.resolve(5, installed(4)).isInstalled)
        assertTrue(AppStatusResolver.resolve(5, installed(5)).isInstalled)
        assertTrue(AppStatusResolver.resolve(5, installed(9)).isInstalled)
    }

    @Test
    fun `огромные versionCode не ломают сравнение`() {
        val status = AppStatusResolver.resolve(
            latestVersionCode = 2_147_483_648L, // больше Int.MAX_VALUE
            installed = installed(2_147_483_647L),
        )
        assertTrue(status is AppStatus.UpdateAvailable)
    }
}
