package com.example.havadurumuapp.data

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File

/** Seçili şehir ve widget için son hava durumu önbelleği. */
class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("mel_prefs", Context.MODE_PRIVATE)

    var city: City?
        get() {
            val raw = prefs.getString("city", null) ?: return null
            return runCatching {
                val o = JSONObject(raw)
                City(o.getString("name"), o.getDouble("lat"), o.getDouble("lon"), o.optString("region"))
            }.getOrNull()
        }
        set(value) {
            if (value == null) return
            val o = JSONObject().put("name", value.name).put("lat", value.lat).put("lon", value.lon)
                .put("region", value.region ?: "")
            prefs.edit().putString("city", o.toString()).apply()
        }

    fun saveSnapshot(w: Weather) {
        prefs.edit()
            .putString("w_city", w.city.name)
            .putFloat("w_temp", w.temp.toFloat())
            .putInt("w_code", w.code)
            .putBoolean("w_day", w.isDay)
            .apply()
    }

    data class Snapshot(val city: String, val temp: Double, val code: Int, val isDay: Boolean)

    val snapshot: Snapshot?
        get() {
            val name = prefs.getString("w_city", null) ?: return null
            return Snapshot(name, prefs.getFloat("w_temp", 0f).toDouble(), prefs.getInt("w_code", 0), prefs.getBoolean("w_day", true))
        }
}

/**
 * Widget'ta gösterilecek fotoğrafın yerel kopyası. Widget her açılışta buluttan
 * indirmesin diye son paylaşım burada saklanır.
 */
class WidgetPhoto(context: Context) {
    private val prefs = context.getSharedPreferences("mel_prefs", Context.MODE_PRIVATE)
    val file = File(context.filesDir, "widget_photo.jpg")

    /** Widget için kullanıcının kendi son paylaşımını, yoksa en yeni paylaşımı seçer. Değiştiyse true. */
    fun update(feed: List<Moment>, uid: String?): Boolean {
        val pick = feed.firstOrNull { it.userId == uid } ?: feed.firstOrNull()
        if (pick == null) return clear()
        if (pick.id == prefs.getString("widget_photo_id", null) && file.exists()) return false
        file.writeBytes(Base64.decode(pick.imageBase64, Base64.DEFAULT))
        prefs.edit().putString("widget_photo_id", pick.id).apply()
        return true
    }

    fun clear(): Boolean {
        val had = file.exists()
        file.delete()
        prefs.edit().remove("widget_photo_id").apply()
        return had
    }
}
