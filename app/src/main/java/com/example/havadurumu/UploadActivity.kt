package com.example.havadurumu

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.max

/** Kamera veya galeriden seçilen gökyüzü fotoğrafını küçültüp Firestore'a kaydeder. */
class UploadActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val arkaPlan = Executors.newSingleThreadExecutor()

    private var secilenBitmap: Bitmap? = null

    private lateinit var ivOnizleme: ImageView
    private lateinit var tvBilgi: TextView
    private lateinit var btnPaylas: Button
    private lateinit var progress: ProgressBar

    private val galeriSec = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            val giris = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(giris)
            giris?.close()
            bitmapGeldi(bmp)
        } catch (e: Exception) {
            toast("Fotoğraf okunamadı: ${e.message}")
        }
    }

    private val kameraCek = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) bitmapGeldi(bmp)
    }

    private val kameraIzni = registerForActivityResult(ActivityResultContracts.RequestPermission()) { verildi ->
        if (verildi) kameraCek.launch(null) else toast("Kamera izni verilmedi")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        ivOnizleme = findViewById(R.id.ivOnizleme)
        tvBilgi = findViewById(R.id.tvBilgi)
        btnPaylas = findViewById(R.id.btnPaylas)
        progress = findViewById(R.id.progress)

        val sehir = intent.getStringExtra("cityName")
        tvBilgi.text = "Bu paylaşım şu konuma eklenecek: ${sehir ?: "bilinmiyor"}"

        findViewById<Button>(R.id.btnKapat).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGaleri).setOnClickListener { galeriSec.launch("image/*") }
        findViewById<Button>(R.id.btnKamera).setOnClickListener { kameraAc() }
        btnPaylas.setOnClickListener { paylas() }
        btnPaylas.isEnabled = false

        camEfektiKur(findViewById(R.id.blurYukleme))

        // Ana ekrandaki speed-dial'dan kamera/galeri seçilmişse otomatik aç
        when (intent.getStringExtra("autoLaunch")) {
            "kamera" -> kameraAc()
            "galeri" -> galeriSec.launch("image/*")
        }
    }

    private fun kameraAc() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            kameraCek.launch(null)
        } else {
            kameraIzni.launch(Manifest.permission.CAMERA)
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

    private fun bitmapGeldi(bmp: Bitmap?) {
        if (bmp == null) { toast("Fotoğraf alınamadı"); return }
        secilenBitmap = bmp
        ivOnizleme.setImageBitmap(bmp)
        btnPaylas.isEnabled = true
    }

    private fun paylas() {
        val bmp = secilenBitmap ?: run { toast("Önce bir fotoğraf seç"); return }
        val kullanici = auth.currentUser ?: run { toast("Giriş yapılmamış"); return }

        val lat = intent.getDoubleExtra("lat", Double.NaN)
        val lon = intent.getDoubleExtra("lon", Double.NaN)
        val sehir = intent.getStringExtra("cityName") ?: ""
        val sicaklik = intent.getDoubleExtra("tempC", 0.0)
        val aciklama = intent.getStringExtra("conditionText") ?: ""
        val ikon = intent.getStringExtra("conditionIcon") ?: ""

        yukleniyor(true)
        arkaPlan.execute {
            val base64 = sikistirVeKodla(bmp)
            runOnUiThread {
                val veri = hashMapOf(
                    "userId" to kullanici.uid,
                    "username" to (kullanici.displayName ?: "kullanici"),
                    "cityName" to sehir,
                    "latitude" to lat,
                    "longitude" to lon,
                    "tempC" to sicaklik,
                    "conditionText" to aciklama,
                    "conditionIcon" to ikon,
                    "imageBase64" to base64,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("posts").add(veri)
                    .addOnSuccessListener {
                        yukleniyor(false)
                        toast("Paylaşıldı!")
                        finish()
                    }
                    .addOnFailureListener { e ->
                        yukleniyor(false)
                        toast("Paylaşılamadı: ${e.message}")
                    }
            }
        }
    }

    /** Fotoğrafı 1080px genişliğe küçültüp, Firestore'un 1MB sınırına güvenle sığacak şekilde sıkıştırır. */
    private fun sikistirVeKodla(kaynak: Bitmap): String {
        val maxGenislik = 1080
        val bmp = if (kaynak.width > maxGenislik) {
            val oran = maxGenislik.toFloat() / kaynak.width
            val yeniYukseklik = max(1, (kaynak.height * oran).toInt())
            Bitmap.createScaledBitmap(kaynak, maxGenislik, yeniYukseklik, true)
        } else kaynak

        var kalite = 80
        var bytes: ByteArray
        do {
            val akis = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, kalite, akis)
            bytes = akis.toByteArray()
            kalite -= 10
        } while (bytes.size > 500_000 && kalite >= 30)

        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    override fun onDestroy() {
        super.onDestroy()
        arkaPlan.shutdownNow()
    }

    private fun yukleniyor(evet: Boolean) {
        progress.visibility = if (evet) View.VISIBLE else View.GONE
        btnPaylas.isEnabled = !evet
    }

    private fun toast(mesaj: String) = Toast.makeText(this, mesaj, Toast.LENGTH_SHORT).show()
}
