package com.personal.appstore.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Человеческие подписи. Чистые функции — покрыты юнит-тестами. */
object Format {

    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")).withZone(ZoneId.systemDefault())

    fun bytes(value: Long): String = when {
        value < 0 -> "—"
        value < 1024 -> "$value Б"
        value < 1024 * 1024 -> String.format(Locale.US, "%.0f КБ", value / 1024.0)
        value < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f МБ", value / (1024.0 * 1024))
        else -> String.format(Locale.US, "%.2f ГБ", value / (1024.0 * 1024 * 1024))
    }

    fun date(instant: Instant?): String = instant?.let(DATE_FORMATTER::format) ?: "—"

    /** «только что» / «12 мин назад» / «3 ч назад» / «5 дн назад». */
    fun ago(millis: Long?, now: Long = System.currentTimeMillis()): String {
        if (millis == null || millis <= 0) return "никогда"
        val delta = abs(now - millis)
        val minutes = delta / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            hours < 24 -> "$hours ч назад"
            days < 30 -> "$days дн назад"
            else -> "давно"
        }
    }
}
