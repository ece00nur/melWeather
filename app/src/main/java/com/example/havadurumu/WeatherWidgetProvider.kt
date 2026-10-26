package com.example.havadurumu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import java.io.File

/** Ana ekrandaki hava durumu widget'ı. Veriyi uygulamanın son yüklediği hava durumundan okur. */
class WeatherWidgetProvider : AppWidgetProvider() {

    companion object {
        const val PREFS = "hava_widget_prefs"

        /** Uygulama tarafında hava durumu güncellenince çağrılır: widget(lar)ı tazeler. */
        fun hepsiniGuncelle(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            ids.forEach { guncelle(context, manager, it) }
        }

        fun guncelle(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val views = RemoteViews(context.packageName, R.layout.widget_hava)

            views.setTextViewText(R.id.tvWidgetSehir, prefs.getString("sehir", "Hava Durumu"))
            views.setTextViewText(R.id.tvWidgetIkon, prefs.getString("ikon", "🌤️"))
            views.setTextViewText(R.id.tvWidgetSicaklik, prefs.getString("sicaklik", "--°"))
            val guncellemeZamani = prefs.getString("zaman", null)
            views.setTextViewText(
                R.id.tvWidgetDurum,
                if (guncellemeZamani != null)
                    "${prefs.getString("aciklama", "")} · $guncellemeZamani"
                else "Uygulamayı açarak güncelle"
            )

            // Kullanıcının widget için seçtiği arka plan fotoğrafı varsa yükle
            val bgDosya = File(context.filesDir, "widget_bg_$widgetId.jpg")
            if (bgDosya.exists()) {
                try {
                    val secenekler = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bmp = BitmapFactory.decodeFile(bgDosya.absolutePath, secenekler)
                    if (bmp != null) views.setImageViewBitmap(R.id.ivWidgetArkaPlan, bmp)
                } catch (_: Exception) { }
            }

            // Widget'a dokununca uygulamayı aç
            val acmaIntent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, widgetId, acmaIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.ivWidgetArkaPlan, pending)

            manager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { guncelle(context, appWidgetManager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { File(context.filesDir, "widget_bg_$it.jpg").delete() }
    }
}
