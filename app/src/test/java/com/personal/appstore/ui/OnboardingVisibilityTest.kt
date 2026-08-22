package com.personal.appstore.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingVisibilityTest {

    @Test
    fun `первый запуск показывает онбординг`() {
        assertTrue(StoreViewModel.shouldShowOnboarding(seen = false, always = false, dismissed = false))
    }

    @Test
    fun `после первого запуска онбординга нет`() {
        assertFalse(StoreViewModel.shouldShowOnboarding(seen = true, always = false, dismissed = false))
    }

    @Test
    fun `режим разработчика возвращает онбординг на каждый запуск`() {
        assertTrue(StoreViewModel.shouldShowOnboarding(seen = true, always = true, dismissed = false))
    }

    @Test
    fun `закрытый онбординг не возвращается внутри сессии`() {
        assertFalse(StoreViewModel.shouldShowOnboarding(seen = true, always = true, dismissed = true))
        assertFalse(StoreViewModel.shouldShowOnboarding(seen = false, always = false, dismissed = true))
    }
}
