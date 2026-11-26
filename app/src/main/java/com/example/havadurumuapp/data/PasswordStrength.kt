package com.example.havadurumuapp.data

/** Şifre güvenlik analizi: uzunluk, karakter çeşitliliğine ve yaygın/kolay şifre listesine göre bir güç skoru üretir. */
enum class PasswordStrengthLevel(val label: String) {
    ZAYIF("Zayıf"),
    ORTA("Orta"),
    GUCLU("Güçlü"),
}

data class PasswordStrength(
    val level: PasswordStrengthLevel,
    val score: Int,
    val maxScore: Int,
    val missing: List<String>,
)

/** En sık kullanılan / en kolay tahmin edilen şifreler (SecLists "10k-most-common" listesinden küçük bir örnek). */
private val COMMON_PASSWORDS = setOf(
    "123456", "123456789", "12345678", "12345", "1234567", "1234567890",
    "password", "password1", "password123", "iloveyou", "qwerty", "qwerty123",
    "111111", "123123", "abc123", "admin", "admin123", "letmein", "welcome",
    "monkey", "dragon", "football", "baseball", "superman", "trustno1",
    "123321", "654321", "000000", "11111111", "asdasd", "asdfgh", "zxcvbn",
    "sifre", "sifre123", "parola", "parola123", "1q2w3e4r", "qazwsx",
)

/** [pass] için güç analizi yapar. Yaygın/kolay bir şifreyse doğrudan Zayıf kabul edilir. */
fun evaluatePassword(pass: String): PasswordStrength {
    val missing = mutableListOf<String>()
    var score = 0

    val isCommon = pass.lowercase() in COMMON_PASSWORDS
    if (isCommon) missing += "çok yaygın kullanılan bir şifre, tahmin edilmesi kolay"

    if (pass.length >= 8) score++ else missing += "en az 8 karakter"
    if (pass.any { it.isUpperCase() }) score++ else missing += "büyük harf"
    if (pass.any { it.isLowerCase() }) score++ else missing += "küçük harf"
    if (pass.any { it.isDigit() }) score++ else missing += "rakam"
    if (pass.any { !it.isLetterOrDigit() }) score++ else missing += "özel karakter (!@#$ gibi)"
    if (pass.length >= 12) score++

    val level = when {
        pass.isEmpty() -> PasswordStrengthLevel.ZAYIF
        isCommon -> PasswordStrengthLevel.ZAYIF
        score <= 2 -> PasswordStrengthLevel.ZAYIF
        score <= 4 -> PasswordStrengthLevel.ORTA
        else -> PasswordStrengthLevel.GUCLU
    }
    return PasswordStrength(level, score, maxScore = 6, missing = missing)
}
