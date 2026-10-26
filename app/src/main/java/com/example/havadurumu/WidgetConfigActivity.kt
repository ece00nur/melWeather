package com.example.havadurumu

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

/** Widget ana ekrana eklenirken açılan, arka plan fotoğrafı seçtiren ekran. */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var secilenBitmap: Bitmap? = null
    private lateinit var ivOnizleme: ImageView

    private val galeriSec = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            val giris = contentResolver.openInputStream(uri)
            bitmapGeldi(BitmapFactory.decodeStream(giris))
            giris?.close()
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

        // Eklenmesi iptal edilirse sistem sonucu "iptal" olarak algılasın
        setResult(Activity.RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_config)
        ivOnizleme = findViewById(R.id.ivOnizleme)

        findViewById<Button>(R.id.btnGaleri).setOnClickListener { galeriSec.launch("image/*") }
        findViewById<Button>(R.id.btnKamera).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                kameraCek.launch(null)
            } else {
                kameraIzni.launch(Manifest.permission.CAMERA)
            }
        }
        findViewById<Button>(R.id.btnEkle).setOnClickListener { kaydetVeBitir(secilenBitmap) }
        findViewById<Button>(R.id.btnVarsayilan).setOnClickListener { kaydetVeBitir(null) }
    }

    private fun bitmapGeldi(bmp: Bitmap?) {
        secilenBitmap = bmp
        ivOnizleme.setImageBitmap(bmp)
    }

    private fun kaydetVeBitir(bmp: Bitmap?) {
        if (bmp != null) {
            try {
                val dosya = File(filesDir, "widget_bg_$widgetId.jpg")
                FileOutputStream(dosya).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, out) }
            } catch (e: Exception) {
                toast("Fotoğraf kaydedilemedi: ${e.message}")
            }
        }

        val manager = AppWidgetManager.getInstance(this)
        WeatherWidgetProvider.guncelle(this, manager, widgetId)

        val sonucIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(Activity.RESULT_OK, sonucIntent)
        finish()
    }

    private fun toast(mesaj: String) = Toast.makeText(this, mesaj, Toast.LENGTH_SHORT).show()
}
