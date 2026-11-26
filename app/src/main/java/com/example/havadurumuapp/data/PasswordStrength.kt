package com.example.havadurumuapp.data

/** Şifre güvenlik analizi: uzunluk ve karakter çeşitliliğine göre basit bir güç skoru üretir. */
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

/** [pass] için güç analizi yapar. Puan arttıkça (büyük/küçük harf, rakam, özel karakter, uzunluk) seviye yükselir. */
fun evaluatePassword(pass: String): PasswordStrength {
    val missing = mutableListOf<String>()
    var score = 0

    if (pass.length >= 8) score++ else missing += "en az 8 karakter"
    if (pass.any { it.isUpperCase() }) score++ else missing += "büyük harf"
    if (pass.any { it.isLowerCase() }) score++ else missing += "küçük harf"
    if (pass.any { it.isDigit() }) score++ else missing += "rakam"
    if (pass.any { !it.isLetterOrDigit() }) score++ else missing += "özel karakter (!@#$ gibi)"
    if (pass.length >= 12) score++

    val level = when {
        pass.isEmpty() -> PasswordStrengthLevel.ZAYIF
        score <= 2 -> PasswordStrengthLevel.ZAYIF
        score <= 4 -> PasswordStrengthLevel.ORTA
        else -> PasswordStrengthLevel.GUCLU
    }
    return PasswordStrength(level, score, maxScore = 6, missing = missing)
}
