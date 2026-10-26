package com.example.havadurumuapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.widget.RemoteViews
import com.example.havadurumuapp.MainActivity
import com.example.havadurumuapp.R
import com.example.havadurumuapp.data.AppPrefs
import com.example.havadurumuapp.data.City
import com.example.havadurumuapp.data.Cloud
import com.example.havadurumuapp.data.Images
import com.example.havadurumuapp.data.WeatherApi
import com.example.havadurumuapp.data.WidgetPhoto
import com.example.havadurumuapp.data.weatherKind
import com.example.havadurumuapp.ui.WeatherArt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Hap (pill) şeklinde widget: üstte son çekilen fotoğraf (daire),
 * altında şehir, sıcaklık, hava ikonu, saat ve tarih.
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids)
        // Arka planda güncel hava durumunu çek, sonra tekrar çiz.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = AppPrefs(context)
                val city: City = prefs.city ?: City("İstanbul", 41.01, 28.97)
                runCatching { WeatherApi.forecast(city) }.getOrNull()?.let { prefs.saveSnapshot(it) }
                // Giriş yapılmışsa buluttaki son paylaşımı da al
                if (Cloud.uid != null) {
                    runCatching { WidgetPhoto(context).update(Cloud.fetchFeedBlocking(), Cloud.uid) }
                }
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        /** Uygulama içinden (yeni fotoğraf / yeni hava verisi) widget'ı yeniler. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val snapshot = AppPrefs(context).snapshot
            val photoFile = WidgetPhoto(context).file
            val density = context.resources.displayMetrics.density
            val photoPx = (120 * density).roundToInt().coerceAtMost(400)

            val photo = photoFile.takeIf { it.exists() }
                ?.let { Images.fromFile(it.path, photoPx) }
                ?.let { Images.circleCrop(it, photoPx) }
                ?: placeholder(photoPx, snapshot?.code ?: 0, snapshot?.isDay ?: true)
            val icon = WeatherArt.bitmap(weatherKind(snapshot?.code ?: 0), snapshot?.isDay ?: true, (28 * density).roundToInt())

            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val views = RemoteViews(context.packageName, R.layout.widget_weather).apply {
                setImageViewBitmap(R.id.widget_photo, photo)
                setImageViewBitmap(R.id.widget_icon, icon)
                setTextViewText(R.id.widget_city, snapshot?.city ?: "MelWeather")
                setTextViewText(R.id.widget_temp, snapshot?.let { "${it.temp.roundToInt()}°C" } ?: "--°C")
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            manager.updateAppWidget(ids, views)
        }

        /** Henüz fotoğraf yoksa gün batımı renklerinde daire + hava ikonu. */
        private fun placeholder(size: Int, code: Int, isDay: Boolean): Bitmap {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, size.toFloat(),
                    intArrayOf(0xFFB48CA8.toInt(), 0xFFF2A28A.toInt(), 0xFFFFC07A.toInt(), 0xFF2C2325.toInt()),
                    floatArrayOf(0f, .45f, .7f, 1f), Shader.TileMode.CLAMP,
                )
            }
            c.drawCircle(size / 2f, size / 2f, size / 2f, p)
            c.save()
            c.translate(size * .2f, size * .18f)
            WeatherArt.draw(c, weatherKind(code), isDay, size * .6f, size * .6f)
            c.restore()
            return bmp
        }
    }
}
