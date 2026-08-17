package com.personal.appstore.data

import java.io.File
import java.security.MessageDigest

/** Проверка целостности APK. Без неё установка не запускается — это жёсткое требование. */
object Sha256 {

    fun newDigest(): MessageDigest = MessageDigest.getInstance("SHA-256")

    fun toHex(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            out.append(HEX[value ushr 4])
            out.append(HEX[value and 0x0F])
        }
        return out.toString()
    }

    fun of(file: File): String {
        val digest = newDigest()
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return toHex(digest.digest())
    }

    /** Сравнение без учёта регистра: в манифесте hex в нижнем регистре, но не полагаемся на это. */
    fun matches(actual: String, expected: String): Boolean = actual.equals(expected, ignoreCase = true)

    private val HEX = "0123456789abcdef".toCharArray()
}
