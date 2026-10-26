package com.example.havadurumuapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

object Images {

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /**
     * Kameradan/galeriden gelen görseli küçültür, yönünü düzeltir ve Firestore'un
     * 1 MB belge sınırına güvenle sığacak şekilde (≤ ~500 KB) JPEG + Base64 yapar.
     */
    fun encodeForUpload(context: Context, uri: Uri, maxSide: Int = 1080): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
        var bmp = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Görsel okunamadı")

        val rotation = runCatching {
            resolver.openInputStream(uri)!!.use { ExifInterface(it).rotationDegrees }
        }.getOrDefault(0)
        if (rotation != 0) {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
        if (scale < 1f) {
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        }

        var quality = 85
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
            quality -= 10
        } while (bytes.size > 500_000 && quality >= 35)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Paylaşımdaki Base64 görseli istenen boyuta yakın çözer (önbellekli). */
    fun fromBase64(id: String, base64: String, reqSide: Int = 600): Bitmap? {
        val key = "$id@$reqSide"
        cache.get(key)?.let { return it }
        val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return null
        return decode(bytes, reqSide)?.also { cache.put(key, it) }
    }

    fun fromFile(path: String, reqSide: Int): Bitmap? {
        val bytes = runCatching { java.io.File(path).readBytes() }.getOrNull() ?: return null
        return decode(bytes, reqSide)
    }

    private fun decode(bytes: ByteArray, reqSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, reqSide) }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    /** Görseli ortadan kırpıp daire şeklinde bitmap yapar (widget için). */
    fun circleCrop(src: Bitmap, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val scale = size.toFloat() / minOf(src.width, src.height)
        val m = Matrix().apply {
            setScale(scale, scale)
            postTranslate((size - src.width * scale) / 2f, (size - src.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(m) }
        }
        Canvas(out).drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return out
    }

    private fun sampleSize(w: Int, h: Int, req: Int): Int {
        var s = 1
        while (maxOf(w, h) / (s * 2) >= req) s *= 2
        return s
    }
}
