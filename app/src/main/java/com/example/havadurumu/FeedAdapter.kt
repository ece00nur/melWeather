package com.example.havadurumu

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import java.text.SimpleDateFormat
import java.util.Locale

/** Tek RecyclerView: 0. satır başlık (hava durumu + arama), sonrası fotoğraf paylaşımları. */
class FeedAdapter(
    private val turkce: Locale,
    private val onAra: () -> Unit,
    private val onKonum: () -> Unit,
    private val onCikis: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIP_BASLIK = 0
        private const val TIP_PAYLASIM = 1
    }

    private val postlar = mutableListOf<Post>()

    // Başlık içindeki görünümlere dışarıdan erişim için
    var baslikView: View? = null
        private set

    override fun getItemViewType(position: Int) = if (position == 0) TIP_BASLIK else TIP_PAYLASIM
    override fun getItemCount() = 1 + postlar.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIP_BASLIK) {
            val v = inflater.inflate(R.layout.item_header, parent, false)
            baslikView = v
            v.findViewById<Button>(R.id.btnAra).setOnClickListener { onAra() }
            v.findViewById<Button>(R.id.btnKonum).setOnClickListener { onKonum() }
            v.findViewById<Button>(R.id.btnCikis).setOnClickListener { onCikis() }
            v.findViewById<EditText>(R.id.etSehir).setOnEditorActionListener { _, id, _ ->
                if (id == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) { onAra(); true } else false
            }
            camEfektleriKur(parent.context, v)
            BaslikTutucu(v)
        } else {
            PaylasimTutucu(inflater.inflate(R.layout.item_post, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PaylasimTutucu) {
            holder.bagla(postlar[position - 1], turkce)
        }
    }

    /** Başlıktaki üç kart için gerçek, canlı arka plan bulanıklaştırma (buzlu cam) efektini kurar. */
    private fun camEfektleriKur(context: android.content.Context, v: View) {
        val activity = context as? Activity ?: return
        val decorView = activity.window.decorView
        val kokView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val algoritma = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            RenderEffectBlur() else RenderScriptBlur(context)

        listOf(R.id.blurHava, R.id.blurTahmin, R.id.blurFeedBos).forEach { id ->
            val blurView = v.findViewById<BlurView>(id) ?: return@forEach
            blurView.setupWith(kokView, algoritma)
                .setFrameClearDrawable(decorView.background)
                .setBlurRadius(18f)
            blurView.setOverlayColor(0xD92C2325L.toInt())
        }
    }

    fun postlariGuncelle(yeni: List<Post>) {
        postlar.clear()
        postlar.addAll(yeni)
        notifyDataSetChanged()
        baslikView?.findViewById<View>(R.id.blurFeedBosKapsayici)?.visibility =
            if (yeni.isEmpty()) View.VISIBLE else View.GONE
    }

    class BaslikTutucu(v: View) : RecyclerView.ViewHolder(v)

    class PaylasimTutucu(v: View) : RecyclerView.ViewHolder(v) {
        private val ivFoto: ImageView = v.findViewById(R.id.ivFoto)
        private val tvKullanici: TextView = v.findViewById(R.id.tvKullanici)
        private val tvHava: TextView = v.findViewById(R.id.tvHava)
        private val tvKonum: TextView = v.findViewById(R.id.tvKonum)
        private val tvSaat: TextView = v.findViewById(R.id.tvSaat)

        fun bagla(post: Post, turkce: Locale) {
            tvKullanici.text = "@${post.username}"
            tvHava.text = "${post.conditionIcon} ${post.tempC.toInt()}°"
            tvKonum.text = post.cityName
            tvSaat.text = post.createdAt?.toDate()?.let {
                SimpleDateFormat("HH:mm", turkce).format(it)
            } ?: ""
            try {
                val bytes = Base64.decode(post.imageBase64, Base64.DEFAULT)
                val bmp: Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ivFoto.setImageBitmap(bmp)
            } catch (_: Exception) {
                ivFoto.setImageBitmap(null)
            }
        }
    }
}
