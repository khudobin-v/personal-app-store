package com.personal.appstore.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `размеры подписываются по-человечески`() {
        assertEquals("512 Б", Format.bytes(512))
        assertEquals("2 КБ", Format.bytes(2048))
        assertEquals("5.0 МБ", Format.bytes(5 * 1024 * 1024))
        assertEquals("1.50 ГБ", Format.bytes((1.5 * 1024 * 1024 * 1024).toLong()))
        assertEquals("—", Format.bytes(-1))
    }

    @Test
    fun `давность считается от текущего момента`() {
        val now = 1_700_000_000_000L
        assertEquals("только что", Format.ago(now - 30_000, now))
        assertEquals("12 мин назад", Format.ago(now - 12 * 60_000, now))
        assertEquals("3 ч назад", Format.ago(now - 3 * 3_600_000, now))
        assertEquals("5 дн назад", Format.ago(now - 5 * 86_400_000L, now))
        assertEquals("давно", Format.ago(now - 90 * 86_400_000L, now))
        assertEquals("никогда", Format.ago(null, now))
    }
}
