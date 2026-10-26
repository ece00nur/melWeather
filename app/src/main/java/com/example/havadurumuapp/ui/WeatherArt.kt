package com.example.havadurumuapp.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.havadurumuapp.data.WeatherKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Hava durumu ikonlarını çizer. Hem Compose ekranlarında hem de widget'ta
 * (bitmap olarak) aynı çizim kullanılır.
 */
object WeatherArt {

    private const val SUN = 0xFFFFC21A.toInt()
    private const val SUN_EDGE = 0xFFFF9F0A.toInt()
    private const val RAIN = 0xFF6FB7F2.toInt()
    private const val BOLT = 0xFFFFC107.toInt()

    fun bitmap(kind: WeatherKind, isDay: Boolean, size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            draw(Canvas(it), kind, isDay, size.toFloat(), size.toFloat())
        }

    fun draw(c: Canvas, kind: WeatherKind, isDay: Boolean, w: Float, h: Float) {
        val s = min(w, h)
        c.save()
        c.translate((w - s) / 2f, (h - s) / 2f)
        when (kind) {
            WeatherKind.CLEAR -> if (isDay) sun(c, s * .5f, s * .5f, s * .26f) else moon(c, s * .5f, s * .5f, s * .3f)
            WeatherKind.PARTLY -> {
                if (isDay) sun(c, s * .62f, s * .36f, s * .2f) else moon(c, s * .64f, s * .34f, s * .2f)
                cloud(c, s * .44f, s * .6f, s * .72f, light = true)
            }
            WeatherKind.CLOUDY -> {
                cloud(c, s * .62f, s * .42f, s * .5f, light = false)
                cloud(c, s * .44f, s * .6f, s * .76f, light = true)
            }
            WeatherKind.FOG -> {
                cloud(c, s * .5f, s * .42f, s * .7f, light = true)
                fog(c, s)
            }
            WeatherKind.DRIZZLE -> {
                cloud(c, s * .5f, s * .42f, s * .76f, light = true)
                drops(c, s, count = 3, long = false)
            }
            WeatherKind.RAIN -> {
                cloud(c, s * .5f, s * .42f, s * .78f, light = false)
                drops(c, s, count = 4, long = true)
            }
            WeatherKind.SNOW -> {
                cloud(c, s * .5f, s * .42f, s * .76f, light = true)
                flakes(c, s)
            }
            WeatherKind.THUNDER -> {
                cloud(c, s * .5f, s * .4f, s * .8f, light = false)
                bolt(c, s)
                drops(c, s, count = 2, long = true)
            }
        }
        c.restore()
    }

    private fun sun(c: Canvas, cx: Float, cy: Float, r: Float) {
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 2.1f, intArrayOf(0x66FFD54F, 0x00FFD54F), null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r * 2.1f, glow)
        val ray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SUN; strokeWidth = r * .16f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            c.drawLine(
                cx + (cos(a) * r * 1.3f).toFloat(), cy + (sin(a) * r * 1.3f).toFloat(),
                cx + (cos(a) * r * 1.65f).toFloat(), cy + (sin(a) * r * 1.65f).toFloat(), ray
            )
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - r * .3f, cy - r * .3f, r * 1.4f, intArrayOf(0xFFFFE07A.toInt(), SUN, SUN_EDGE), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r, body)
    }

    private fun moon(c: Canvas, cx: Float, cy: Float, r: Float) {
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 1.9f, intArrayOf(0x55F3E7EA, 0x00F3E7EA), null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r * 1.9f, glow)
        val outer = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        val cut = Path().apply { addCircle(cx + r * .55f, cy - r * .35f, r * .85f, Path.Direction.CW) }
        outer.op(cut, Path.Op.DIFFERENCE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xFFFFF6E0.toInt(), 0xFFE8C9A0.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawPath(outer, p)
    }

    /** Kabarık 3B görünümlü bulut. (cx, cy) bulutun merkezi, w genişliği. */
    private fun cloud(c: Canvas, cx: Float, cy: Float, w: Float, light: Boolean) {
        val h = w * .56f
        val left = cx - w / 2f
        val bottom = cy + h / 2f
        val path = Path().apply {
            addRoundRect(left, bottom - h * .48f, left + w, bottom, h * .24f, h * .24f, Path.Direction.CW)
            addCircle(left + w * .3f, bottom - h * .48f, w * .2f, Path.Direction.CW)
            addCircle(left + w * .57f, bottom - h * .62f, w * .27f, Path.Direction.CW)
            addCircle(left + w * .8f, bottom - h * .4f, w * .16f, Path.Direction.CW)
        }
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000 }
        c.save(); c.translate(0f, h * .06f); c.drawPath(path, shadow); c.restore()

        val top = if (light) 0xFFFFFFFF.toInt() else 0xFFF1ECEE.toInt()
        val bot = if (light) 0xFFE4DADD.toInt() else 0xFFA99A9E.toInt()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, bottom - h * 1.1f, 0f, bottom, top, bot, Shader.TileMode.CLAMP)
        }
        c.drawPath(path, fill)
        val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(left + w * .5f, bottom - h * .85f, w * .35f, intArrayOf(0x88FFFFFF.toInt(), 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(left + w * .5f, bottom - h * .85f, w * .35f, shine)
    }

    private fun drops(c: Canvas, s: Float, count: Int, long: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RAIN; strokeWidth = s * .045f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        val len = if (long) s * .13f else s * .07f
        val start = s * .5f - (count - 1) * s * .13f / 2f
        for (i in 0 until count) {
            val x = start + i * s * .13f
            val y = s * .74f + (i % 2) * s * .05f
            c.drawLine(x, y, x - len * .35f, y + len, p)
        }
    }

    private fun flakes(c: Canvas, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFDDEBFA.toInt(); strokeWidth = s * .03f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        listOf(.34f to .8f, .5f to .86f, .66f to .8f).forEach { (fx, fy) ->
            val x = s * fx; val y = s * fy; val r = s * .05f
            for (k in 0 until 3) {
                val a = Math.toRadians(k * 60.0)
                c.drawLine(x - (cos(a) * r).toFloat(), y - (sin(a) * r).toFloat(), x + (cos(a) * r).toFloat(), y + (sin(a) * r).toFloat(), p)
            }
        }
    }

    private fun fog(c: Canvas, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xDDE9E1E3.toInt(); strokeWidth = s * .045f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        c.drawLine(s * .22f, s * .76f, s * .7f, s * .76f, p)
        c.drawLine(s * .32f, s * .85f, s * .8f, s * .85f, p)
    }

    private fun bolt(c: Canvas, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BOLT }
        val path = Path().apply {
            moveTo(s * .53f, s * .58f)
            lineTo(s * .42f, s * .76f)
            lineTo(s * .5f, s * .76f)
            lineTo(s * .45f, s * .92f)
            lineTo(s * .62f, s * .7f)
            lineTo(s * .53f, s * .7f)
            lineTo(s * .58f, s * .58f)
            close()
        }
        c.drawPath(path, p)
    }
}
