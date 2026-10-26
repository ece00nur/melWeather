package com.example.havadurumu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur

/** Basit kullanıcı adı + şifre ile giriş/kayıt ekranı (e-posta doğrulama yok). */
class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    private lateinit var etKullaniciAdi: EditText
    private lateinit var etSifre: EditText
    private lateinit var tvHata: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tvBaslik: TextView
    private lateinit var btnGonder: Button
    private lateinit var btnGecis: Button

    /** true: "Hesap Oluştur" modunda, false: "Giriş Yap" modunda. */
    private var kayitModu = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Zaten giriş yapılmışsa doğrudan ana ekrana geç
        if (auth.currentUser != null) {
            anaEkraniAc()
            return
        }

        etKullaniciAdi = findViewById(R.id.etKullaniciAdi)
        etSifre = findViewById(R.id.etSifre)
        tvHata = findViewById(R.id.tvHata)
        progress = findViewById(R.id.progress)
        tvBaslik = findViewById(R.id.tvBaslik)
        btnGonder = findViewById(R.id.btnGiris)
        btnGecis = findViewById(R.id.btnKayit)

        btnGonder.setOnClickListener { if (kayitModu) kayitOl() else girisYap() }
        btnGecis.setOnClickListener {
            kayitModu = !kayitModu
            modaGoreGuncelle()
        }

        camEfektiKur(findViewById(R.id.blurGiris))
    }

    /** Başlık, buton ve geçiş yazısını seçili moda (giriş/kayıt) göre günceller. */
    private fun modaGoreGuncelle() {
        tvHata.visibility = View.GONE
        if (kayitModu) {
            tvBaslik.text = "Hesap Oluştur"
            btnGonder.text = "Hesap Oluştur"
            btnGecis.text = "Hesabın var mı? Hemen giriş yap."
        } else {
            tvBaslik.text = "Giriş Yap"
            btnGonder.text = "Giriş Yap"
            btnGecis.text = "Hesabın yok mu? Hemen bir tane oluştur."
        }
    }

    /** Gerçek, canlı arka plan bulanıklaştırma (buzlu cam) efektini kurar. */
    private fun camEfektiKur(blurView: BlurView) {
        val decorView = window.decorView
        val kokView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val algoritma = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            RenderEffectBlur() else RenderScriptBlur(this)

        blurView.setupWith(kokView, algoritma)
            .setFrameClearDrawable(decorView.background)
            .setBlurRadius(18f)
        blurView.setOverlayColor(0xD92C2325L.toInt())
    }

    /** Kullanıcı adını Firebase Authentication'ın istediği e-posta biçimine çevirir. */
    private fun sahteEmail(kullaniciAdi: String) =
        kullaniciAdi.trim().lowercase().replace(Regex("[^a-z0-9._-]"), "") + "@havadurumu.app"

    private fun girisBilgileriniAl(): Pair<String, String>? {
        val ad = etKullaniciAdi.text.toString().trim()
        val sifre = etSifre.text.toString()
        tvHata.visibility = View.GONE
        if (ad.length < 3) {
            hataGoster("Kullanıcı adı en az 3 karakter olmalı")
            return null
        }
        if (sifre.length < 6) {
            hataGoster("Şifre en az 6 karakter olmalı")
            return null
        }
        return ad to sifre
    }

    private fun girisYap() {
        val (ad, sifre) = girisBilgileriniAl() ?: return
        yukleniyor(true)
        auth.signInWithEmailAndPassword(sahteEmail(ad), sifre)
            .addOnSuccessListener { anaEkraniAc() }
            .addOnFailureListener {
                yukleniyor(false)
                hataGoster("Giriş başarısız: kullanıcı adı veya şifre yanlış, ya da önce kayıt olmalısın.")
            }
    }

    private fun kayitOl() {
        val (ad, sifre) = girisBilgileriniAl() ?: return
        yukleniyor(true)
        auth.createUserWithEmailAndPassword(sahteEmail(ad), sifre)
            .addOnSuccessListener { sonuc ->
                val profil = UserProfileChangeRequest.Builder().setDisplayName(ad).build()
                sonuc.user?.updateProfile(profil)?.addOnCompleteListener { anaEkraniAc() }
                    ?: anaEkraniAc()
            }
            .addOnFailureListener { e ->
                yukleniyor(false)
                val mesaj = if (e.message?.contains("already in use") == true)
                    "Bu kullanıcı adı zaten alınmış, giriş yapmayı dene."
                else "Kayıt başarısız: ${e.message}"
                hataGoster(mesaj)
            }
    }

    private fun anaEkraniAc() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun hataGoster(mesaj: String) {
        tvHata.text = mesaj
        tvHata.visibility = View.VISIBLE
    }

    private fun yukleniyor(evet: Boolean) {
        progress.visibility = if (evet) View.VISIBLE else View.GONE
    }
}
