package com.example.havadurumuapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Kolay/yaygın şifrelerin ve güçlü şifrelerin doğru sınıflandırıldığını kanıtlayan analiz testleri. */
class PasswordStrengthTest {

    @Test
    fun `yaygın kolay şifreler zayıf olarak işaretlenir`() {
        val easyPasswords = listOf("123456", "password", "qwerty123", "admin123", "sifre123", "11111111")
        easyPasswords.forEach { pass ->
            assertEquals(
                "'$pass' zayıf çıkmalıydı",
                PasswordStrengthLevel.ZAYIF,
                evaluatePassword(pass).level,
            )
        }
    }

    @Test
    fun `kısa veya tek tip şifreler zayıf olarak işaretlenir`() {
        val weakPasswords = listOf("abc", "12345", "aaaaaaaa", "")
        weakPasswords.forEach { pass ->
            assertEquals(
                "'$pass' zayıf çıkmalıydı",
                PasswordStrengthLevel.ZAYIF,
                evaluatePassword(pass).level,
            )
        }
    }

    @Test
    fun `orta seviye şifreler doğru sınıflandırılır`() {
        assertEquals(PasswordStrengthLevel.ORTA, evaluatePassword("Deneme123").level)
    }

    @Test
    fun `uzun ve çeşitli şifreler güçlü olarak işaretlenir`() {
        val strongPasswords = listOf("K8!veri#Tut3", "Gunes2026!zm", "Tr@bzon5poR!")
        strongPasswords.forEach { pass ->
            assertEquals(
                "'$pass' güçlü çıkmalıydı",
                PasswordStrengthLevel.GUCLU,
                evaluatePassword(pass).level,
            )
        }
    }
}
