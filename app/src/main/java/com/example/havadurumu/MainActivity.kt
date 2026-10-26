package com.example.havadurumu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val turkce: Locale = Locale.forLanguageTag("tr-TR")
    private val arkaPlan = Executors.newSingleThreadExecutor()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: FeedAdapter
    private lateinit var swipe: SwipeRefreshLayout

    // Ekranda gösterilen yerin bilgileri — fotoğraf paylaşırken de kullanılır
    var sonEnlem: Double? = null; private set
    var sonBoylam: Double? = null; private set
    var sonSehirAdi: String = ""; private set
    var sonSicaklik: Double = 0.0; private set
    var sonAciklama: String = ""; private set
    var sonIkon: String = ""; private set

    private val izinIste = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { sonuc ->
        if (sonuc.values.any { it }) konumuBul()
        else { durum("Konum izni verilmedi. Varsayılan olarak İstanbul gösteriliyor."); sehirAra("İstanbul") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        swipe = findViewById(R.id.swipe)
        swipe.setOnRefreshListener { konumuBul(); feedDinle() }

        adapter = FeedAdapter(
            turkce,
            onAra = {
                val baslik = adapter.baslikView ?: return@FeedAdapter
                val ad = baslik.findViewById<EditText>(R.id.etSehir).text.toString().trim()
                if (ad.isEmpty()) toast("Lütfen bir şehir adı yazın") else sehirAra(ad)
            },
            onKonum = { konumuBul() },
            onCikis = {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        )
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFeed)
        val gridYoneticisi = GridLayoutManager(this, 2)
        gridYoneticisi.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (position == 0) 2 else 1
        }
        rv.layoutManager = gridYoneticisi
        rv.adapter = adapter

        speedDialKur()

        konumuBul()
        feedDinle()
    }

    override fun onResume() {
        super.onResume()
        adapter.baslikView?.findViewById<TextView>(R.id.tvKullanici)?.text =
            "👋 ${auth.currentUser?.displayName ?: "Kullanıcı"}"
    }

    override fun onDestroy() {
        super.onDestroy()
        arkaPlan.shutdownNow()
    }

    // -------------------------------------------------------- SPEED DIAL

    private var speedDialAcik = false

    /** "+" butonuna basınca kamera/galeri mini butonlarının açılıp kapanmasını kurar. */
    private fun speedDialKur() {
        val btnEkle = findViewById<View>(R.id.btnEkle)
        val tvIkon = findViewById<TextView>(R.id.tvEkleIkon)
        val btnMiniKamera = findViewById<View>(R.id.btnMiniKamera)
        val btnMiniGaleri = findViewById<View>(R.id.btnMiniGaleri)

        btnEkle.setOnClickListener {
            if (sonEnlem == null) {
                toast("Önce hava durumunun yüklenmesini bekle")
                return@setOnClickListener
            }
            speedDialAcik = !speedDialAcik
            btnMiniKamera.visibility = if (speedDialAcik) View.VISIBLE else View.GONE
            btnMiniGaleri.visibility = if (speedDialAcik) View.VISIBLE else View.GONE
            tvIkon.text = if (speedDialAcik) "✕" else "↗"
        }
        btnMiniKamera.setOnClickListener { yuklemeEkraniniAc("kamera") }
        btnMiniGaleri.setOnClickListener { yuklemeEkraniniAc("galeri") }
    }

    private fun yuklemeEkraniniAc(kaynak: String) {
        val i = Intent(this, UploadActivity::class.java)
        i.putExtra("lat", sonEnlem)
        i.putExtra("lon", sonBoylam)
        i.putExtra("cityName", sonSehirAdi)
        i.putExtra("tempC", sonSicaklik)
        i.putExtra("conditionText", sonAciklama)
        i.putExtra("conditionIcon", sonIkon)
        i.putExtra("autoLaunch", kaynak)
        startActivity(i)
        speedDialAcik = false
        findViewById<View>(R.id.btnMiniKamera).visibility = View.GONE
        findViewById<View>(R.id.btnMiniGaleri).visibility = View.GONE
        findViewById<TextView>(R.id.tvEkleIkon).text = "↗"
    }

    // ------------------------------------------------------------- FEED

    private fun feedDinle() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { anlik, hata ->
                swipe.isRefreshing = false
                if (hata != null || anlik == null) return@addSnapshotListener
                val liste = anlik.documents.mapNotNull { d ->
                    d.toObject(Post::class.java)?.copy(postId = d.id)
                }
                adapter.postlariGuncelle(liste)
            }
    }

    // ---------------------------------------------------------------- KONUM

    @SuppressLint("MissingPermission")
    private fun konumuBul() {
        val ince = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val kaba = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!ince && !kaba) {
            izinIste.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }

        durum("Konumunuz bulunuyor…")
        yukleniyor(true)
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val saglayicilar = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { lm.isProviderEnabled(it) }
        if (saglayicilar.isEmpty()) {
            yukleniyor(false)
            durum("Telefonun konum servisi kapalı. Lütfen konumu açıp \"Konumum\" butonuna basın.")
            return
        }

        val sonBilinen = saglayicilar.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (sonBilinen != null && System.currentTimeMillis() - sonBilinen.time < 5 * 60 * 1000) {
            konumIcinYukle(sonBilinen); return
        }

        var bulundu = false
        val sonucGeldi: (Location?) -> Unit = { loc ->
            if (!bulundu && loc != null) { bulundu = true; konumIcinYukle(loc) }
        }
        for (p in saglayicilar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lm.getCurrentLocation(p, null, mainExecutor) { sonucGeldi(it) }
            } else {
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(p, object : LocationListener {
                    override fun onLocationChanged(location: Location) = sonucGeldi(location)
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }, Looper.getMainLooper())
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (!bulundu) {
                bulundu = true
                if (sonBilinen != null) konumIcinYukle(sonBilinen)
                else { yukleniyor(false); durum("Konum alınamadı. Varsayılan olarak İstanbul gösteriliyor."); sehirAra("İstanbul") }
            }
        }, 15_000)
    }

    private fun konumIcinYukle(loc: Location) {
        val lat = loc.latitude; val lon = loc.longitude
        arkaPlanda {
            val ad = yerAdiBul(lat, lon)
            havaDurumuYukle(lat, lon, "📍 $ad")
        }
    }

    private fun yerAdiBul(lat: Double, lon: Double): String {
        try {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                val a = Geocoder(this, turkce).getFromLocation(lat, lon, 1)?.firstOrNull()
                if (a != null) {
                    val parcalar = listOf(a.subAdminArea ?: a.locality, a.adminArea, a.countryName)
                        .filter { !it.isNullOrBlank() }.distinct()
                    if (parcalar.isNotEmpty()) return parcalar.joinToString(", ")
                }
            }
        } catch (_: Exception) { }
        try {
            val j = JSONObject(httpGet("https://nominatim.openstreetmap.org/reverse?format=json&zoom=10&accept-language=tr&lat=$lat&lon=$lon"))
            val adr = j.getJSONObject("address")
            val ilce = adr.optString("town").ifEmpty { adr.optString("city_district") }
            val il = adr.optString("city").ifEmpty { adr.optString("province") }.ifEmpty { adr.optString("state") }
            val parcalar = listOf(ilce, il, adr.optString("country")).filter { it.isNotBlank() }.distinct()
            if (parcalar.isNotEmpty()) return parcalar.joinToString(", ")
        } catch (_: Exception) { }
        return String.format(Locale.US, "%.4f, %.4f", lat, lon)
    }

    // ------------------------------------------------------------ ŞEHİR ARA

    private fun sehirAra(ad: String) {
        durum("\"$ad\" aranıyor…")
        arkaPlanda {
            val url = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=tr&format=json&name=" +
                    URLEncoder.encode(ad, "UTF-8")
            val sonuclar = JSONObject(httpGet(url)).optJSONArray("results")
            if (sonuclar == null || sonuclar.length() == 0) {
                runOnUiThread { durum("\"$ad\" adında bir yer bulunamadı.") }
                return@arkaPlanda
            }
            val s = sonuclar.getJSONObject(0)
            val yerAdi = listOf(s.optString("name"), s.optString("admin1"), s.optString("country"))
                .filter { it.isNotBlank() }.distinct().joinToString(", ")
            havaDurumuYukle(s.getDouble("latitude"), s.getDouble("longitude"), yerAdi)
        }
    }

    // ---------------------------------------------------------- HAVA DURUMU

    private fun havaDurumuYukle(lat: Double, lon: Double, yerAdi: String) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code," +
                "wind_speed_10m,wind_direction_10m,surface_pressure,precipitation" +
                "&hourly=temperature_2m,weather_code,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=5"
        val j = JSONObject(httpGet(url))
        val c = j.getJSONObject("current")
        val kod = c.getInt("weather_code")
        val gunduz = c.optInt("is_day", 1) == 1
        val (aciklama, ikon) = havaKodu(kod, gunduz)

        val nemMetni = "💧 Nem\n%${c.getInt("relative_humidity_2m")}"
        val ruzgarMetni = "🌬️ Rüzgar\n${c.getDouble("wind_speed_10m")} km/s ${yon(c.getDouble("wind_direction_10m"))}"
        val basincMetni = "⏱️ Basınç\n${c.getDouble("surface_pressure").roundToInt()} hPa"
        val kaynakMetni = "Koordinat: ${String.format(Locale.US, "%.4f, %.4f", lat, lon)}  •  " +
                "Ölçüm: ${c.getString("time").replace("T", " ")} (${j.optString("timezone")})\nKaynak: Open-Meteo (api.open-meteo.com)"

        val d = j.getJSONObject("daily")
        val gunler = d.getJSONArray("time")
        val gunSatirlari = mutableListOf<String>()
        for (i in 0 until gunler.length()) {
            val (gAciklama, gIkon) = havaKodu(d.getJSONArray("weather_code").getInt(i), true)
            val min = d.getJSONArray("temperature_2m_min").getDouble(i).roundToInt()
            val max = d.getJSONArray("temperature_2m_max").getDouble(i).roundToInt()
            val yagis = d.getJSONArray("precipitation_probability_max").optInt(i, 0)
            gunSatirlari += "$gIkon  ${gunAdi(gunler.getString(i))}: $min° / $max°  •  $gAciklama  •  ☔ %$yagis"
        }
        val tahmin = gunSatirlari.joinToString("\n")

        // Saatlik şerit: şu andan itibaren gelecek 6 saat
        val h = j.getJSONObject("hourly")
        val saatler = h.getJSONArray("time")
        val simdikiSaat = c.getString("time") // "2026-10-16T15:00" biçiminde
        var baslangic = (0 until saatler.length()).indexOfFirst { saatler.getString(it) >= simdikiSaat }
        if (baslangic < 0) baslangic = 0
        data class SaatVerisi(val saat: String, val ikon: String)
        val saatlikListe = mutableListOf<SaatVerisi>()
        var idx = baslangic
        while (saatlikListe.size < 6 && idx < saatler.length()) {
            val sGunduz = h.getJSONArray("is_day").optInt(idx, 1) == 1
            val (_, sIkon) = havaKodu(h.getJSONArray("weather_code").getInt(idx), sGunduz)
            val saatMetni = saatler.getString(idx).substringAfter("T")
            saatlikListe += SaatVerisi(saatMetni, sIkon)
            idx++
        }

        val temp = c.getDouble("temperature_2m")

        runOnUiThread {
            sonEnlem = lat; sonBoylam = lon; sonSehirAdi = yerAdi
            sonSicaklik = temp; sonAciklama = aciklama; sonIkon = ikon

            val b = adapter.baslikView
            if (b != null) {
                b.findViewById<TextView>(R.id.tvSehir).text = yerAdi
                b.findViewById<TextView>(R.id.tvIkon).text = ikon
                b.findViewById<TextView>(R.id.tvSicaklik).text = "${temp.roundToInt()}°C"
                b.findViewById<TextView>(R.id.tvAciklama).text = aciklama
                b.findViewById<TextView>(R.id.tvNem).text = nemMetni
                b.findViewById<TextView>(R.id.tvRuzgar).text = ruzgarMetni
                b.findViewById<TextView>(R.id.tvBasinc).text = basincMetni
                b.findViewById<TextView>(R.id.tvKaynakBilgi).text = kaynakMetni
                b.findViewById<TextView>(R.id.tvTahmin).text = tahmin.toString()

                val saatlikContainer = b.findViewById<android.widget.LinearLayout>(R.id.llSaatlik)
                saatlikContainer.removeAllViews()
                val inflater = layoutInflater
                saatlikListe.forEach { sv ->
                    val chip = inflater.inflate(R.layout.item_hour_chip, saatlikContainer, false)
                    chip.findViewById<TextView>(R.id.tvSaatIkon).text = sv.ikon
                    chip.findViewById<TextView>(R.id.tvSaat).text = sv.saat
                    saatlikContainer.addView(chip)
                }
            }
            val zamanMetni = java.text.SimpleDateFormat("HH:mm:ss", turkce).format(java.util.Date())
            durum("Güncellendi: $zamanMetni")
            yukleniyor(false)

            // Ana ekran widget'ını da aynı veriyle güncelle
            getSharedPreferences(WeatherWidgetProvider.PREFS, MODE_PRIVATE).edit()
                .putString("sehir", yerAdi)
                .putString("ikon", ikon)
                .putString("sicaklik", "${temp.roundToInt()}°C")
                .putString("aciklama", aciklama)
                .putString("zaman", zamanMetni)
                .apply()
            WeatherWidgetProvider.hepsiniGuncelle(this)
        }
    }

    private fun havaKodu(kod: Int, gunduz: Boolean): Pair<String, String> = when (kod) {
        0 -> "Açık" to (if (gunduz) "☀️" else "🌙")
        1 -> "Çoğunlukla açık" to (if (gunduz) "🌤️" else "🌙")
        2 -> "Parçalı bulutlu" to "⛅"
        3 -> "Kapalı" to "☁️"
        45, 48 -> "Sisli" to "🌫️"
        51, 53, 55 -> "Çisenti" to "🌦️"
        56, 57 -> "Donan çisenti" to "🌧️"
        61 -> "Hafif yağmurlu" to "🌦️"
        63 -> "Yağmurlu" to "🌧️"
        65 -> "Şiddetli yağmur" to "🌧️"
        66, 67 -> "Donan yağmur" to "🌧️"
        71 -> "Hafif kar yağışlı" to "🌨️"
        73 -> "Kar yağışlı" to "🌨️"
        75 -> "Yoğun kar yağışlı" to "❄️"
        77 -> "Kar taneleri" to "🌨️"
        80, 81 -> "Sağanak yağış" to "🌦️"
        82 -> "Şiddetli sağanak" to "⛈️"
        85, 86 -> "Kar sağanağı" to "🌨️"
        95 -> "Gök gürültülü fırtına" to "⛈️"
        96, 99 -> "Dolulu fırtına" to "⛈️"
        else -> "Bilinmiyor ($kod)" to "🌡️"
    }

    private fun yon(derece: Double): String {
        val yonler = arrayOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
        return yonler[((derece / 45.0).roundToInt()) % 8]
    }

    private fun gunAdi(tarih: String): String = try {
        val t = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(tarih)!!
        java.text.SimpleDateFormat("d MMM EEEE", turkce).format(t)
    } catch (_: Exception) { tarih }

    // ------------------------------------------------------------ YARDIMCI

    private fun httpGet(adres: String): String {
        val c = URL(adres).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", "HavaDurumuOgrenciUygulamasi/1.0 (Android)")
        try {
            val kod = c.responseCode
            if (kod == 429) throw IOException("Çok fazla istek (429). Biraz bekleyip tekrar deneyin.")
            if (kod !in 200..299) throw IOException("Sunucu hatası: HTTP $kod")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    private fun arkaPlanda(gorev: () -> Unit) {
        yukleniyor(true)
        arkaPlan.execute {
            try { gorev() }
            catch (e: Exception) { runOnUiThread { durum("Hata: ${e.message ?: e.javaClass.simpleName} — internet bağlantısını kontrol edin.") } }
            finally { runOnUiThread { yukleniyor(false) } }
        }
    }

    private fun yukleniyor(evet: Boolean) {
        adapter.baslikView?.findViewById<ProgressBar>(R.id.progress)?.visibility = if (evet) View.VISIBLE else View.GONE
    }

    private fun durum(mesaj: String) {
        adapter.baslikView?.findViewById<TextView>(R.id.tvDurum)?.text = mesaj
    }

    private fun toast(mesaj: String) = Toast.makeText(this, mesaj, Toast.LENGTH_SHORT).show()
}
