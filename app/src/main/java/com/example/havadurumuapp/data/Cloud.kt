package com.example.havadurumuapp.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Firebase bağlantısı.
 *  - Hesaplar: Firebase Authentication (kullanıcı adı → "ad@havadurumu.app" e-postası).
 *    Eski HavaDurumu uygulamasıyla aynı yöntem, yani aynı hesaplar iki uygulamada da çalışır.
 *  - Paylaşımlar: Firestore "posts" koleksiyonu, fotoğraf Base64 metin olarak saklanır
 *    (Firebase'in ücretsiz Spark planında Storage gerekmeden çalışır).
 */
object Cloud {

    private const val POSTS = "posts"
    private const val FEED_LIMIT = 40L

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    val uid: String? get() = auth.currentUser?.uid

    val username: String?
        get() = auth.currentUser?.let { it.displayName?.ifBlank { null } ?: it.email?.substringBefore("@") }

    private fun email(name: String) =
        name.trim().lowercase().replace(Regex("[^a-z0-9._-]"), "") + "@havadurumu.app"

    private fun validate(name: String, pass: String): String? = when {
        name.trim().length < 3 -> "Kullanıcı adı en az 3 karakter olmalı"
        email(name).substringBefore("@").isEmpty() -> "Kullanıcı adında İngilizce harf veya rakam kullan"
        pass.length < 6 -> "Şifre en az 6 karakter olmalı"
        else -> null
    }

    /** Başarılıysa null, değilse kullanıcıya gösterilecek hata mesajı döner. */
    suspend fun login(name: String, pass: String): String? {
        validate(name, pass)?.let { return it }
        return try {
            auth.signInWithEmailAndPassword(email(name), pass).await()
            null
        } catch (e: Exception) {
            errorMessage(e)
        }
    }

    suspend fun register(name: String, pass: String): String? {
        validate(name, pass)?.let { return it }
        val strength = evaluatePassword(pass)
        if (strength.level == PasswordStrengthLevel.ZAYIF) {
            return "Şifre zayıf. Eksik: ${strength.missing.joinToString(", ")}"
        }
        return try {
            val result = auth.createUserWithEmailAndPassword(email(name), pass).await()
            result.user?.updateProfile(userProfileChangeRequest { displayName = name.trim() })?.await()
            null
        } catch (e: Exception) {
            errorMessage(e)
        }
    }

    fun logout() = auth.signOut()

    private fun errorMessage(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "Bu kullanıcı adı zaten alınmış"
        is FirebaseAuthWeakPasswordException -> "Şifre çok zayıf, en az 6 karakter kullan"
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Kullanıcı adı veya şifre hatalı"
        is FirebaseNetworkException -> "İnternet bağlantısı yok"
        else -> "Bir hata oluştu: ${e.localizedMessage}"
    }

    // ---------- Paylaşımlar ----------

    private fun feedQuery() = db.collection(POSTS)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(FEED_LIMIT)

    /** Paylaşımları canlı dinler; biri yeni fotoğraf paylaşınca liste kendiliğinden güncellenir. */
    fun listenFeed(onChange: (List<Moment>) -> Unit, onError: (String) -> Unit): ListenerRegistration =
        feedQuery().addSnapshotListener { snap, err ->
            if (err != null) {
                onError("Paylaşımlar yüklenemedi: ${err.localizedMessage}")
                return@addSnapshotListener
            }
            onChange(snap?.documents.orEmpty().mapNotNull(::parse))
        }

    /** Widget gibi arka plan işleri için tek seferlik okuma (ana thread dışında çağrılmalı). */
    fun fetchFeedBlocking(): List<Moment> =
        Tasks.await(feedQuery().get(), 15, TimeUnit.SECONDS).documents.mapNotNull(::parse)

    suspend fun share(jpegBase64: String, w: Weather?) {
        val user = auth.currentUser ?: error("Giriş yapılmamış")
        val data = hashMapOf(
            "userId" to user.uid,
            "username" to (username ?: "kullanici"),
            "cityName" to (w?.city?.name ?: ""),
            "latitude" to (w?.city?.lat ?: 0.0),
            "longitude" to (w?.city?.lon ?: 0.0),
            "tempC" to (w?.temp ?: 0.0),
            "conditionText" to (w?.let { weatherLabel(it.code) } ?: ""),
            "conditionIcon" to "",
            "weatherCode" to (w?.code ?: 0),
            "isDay" to (w?.isDay ?: true),
            "imageBase64" to jpegBase64,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        db.collection(POSTS).add(data).await()
    }

    suspend fun delete(id: String) {
        db.collection(POSTS).document(id).delete().await()
    }

    private fun parse(d: DocumentSnapshot): Moment? {
        val image = d.getString("imageBase64")?.takeIf { it.isNotEmpty() } ?: return null
        val text = d.getString("conditionText").orEmpty()
        val time = d.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        return Moment(
            id = d.id,
            userId = d.getString("userId").orEmpty(),
            user = d.getString("username") ?: "kullanici",
            city = d.getString("cityName")?.removePrefix("📍")?.trim()?.ifBlank { null } ?: "—",
            temp = d.getDouble("tempC") ?: 0.0,
            code = d.getLong("weatherCode")?.toInt() ?: codeFromText(text),
            isDay = d.getBoolean("isDay") ?: true,
            time = time?.toDate()?.time ?: 0L,
            imageBase64 = image,
        )
    }

    /** Eski uygulamadan gelen paylaşımlarda hava kodu yok; açıklamadan tahmin edilir. */
    private fun codeFromText(t: String): Int {
        val s = t.lowercase()
        return when {
            "fırtına" in s || "gök gürültü" in s || "thunder" in s -> 95
            "kar" in s || "snow" in s -> 71
            "yağmur" in s || "sağanak" in s || "rain" in s || "shower" in s -> 61
            "çisel" in s || "drizzle" in s -> 51
            "sis" in s || "pus" in s || "fog" in s || "mist" in s -> 45
            "kapalı" in s || "overcast" in s -> 3
            "bulut" in s || "cloud" in s -> 2
            "açık" in s || "güneş" in s || "clear" in s || "sunny" in s -> 0
            else -> 2
        }
    }
}
