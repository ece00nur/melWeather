package com.example.havadurumuapp

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.havadurumuapp.data.AppPrefs
import com.example.havadurumuapp.data.City
import com.example.havadurumuapp.data.Cloud
import com.example.havadurumuapp.data.Images
import com.example.havadurumuapp.data.LocationHelper
import com.example.havadurumuapp.data.Moment
import com.example.havadurumuapp.data.Weather
import com.example.havadurumuapp.data.WeatherApi
import com.example.havadurumuapp.data.WidgetPhoto
import com.example.havadurumuapp.widget.WeatherWidgetProvider
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPrefs(app)
    private val widgetPhoto = WidgetPhoto(app)

    var user by mutableStateOf(Cloud.username)
        private set
    val uid: String? get() = Cloud.uid

    var weather by mutableStateOf<Weather?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var query by mutableStateOf("")
        private set
    var suggestions by mutableStateOf<List<City>>(emptyList())
        private set
    var moments by mutableStateOf<List<Moment>>(emptyList())
        private set
    var uploading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    private var searchJob: Job? = null
    private var feed: ListenerRegistration? = null
    private var started = false

    init {
        if (user != null) startFeed()
    }

    override fun onCleared() {
        feed?.remove()
    }

    // ---------- Hesap (Firebase Authentication) ----------

    suspend fun login(name: String, pass: String): String? = Cloud.login(name, pass).also { if (it == null) onSignedIn() }

    suspend fun register(name: String, pass: String): String? = Cloud.register(name, pass).also { if (it == null) onSignedIn() }

    private fun onSignedIn() {
        user = Cloud.username
        startFeed()
    }

    fun logout() {
        feed?.remove()
        feed = null
        Cloud.logout()
        user = null
        moments = emptyList()
        if (widgetPhoto.clear()) WeatherWidgetProvider.refreshAll(getApplication())
    }

    // ---------- Hava durumu ----------

    /** İlk açılış: önce cihaz konumu, alınamazsa son seçilen şehir, o da yoksa İstanbul. */
    fun start(locationGranted: Boolean) {
        if (started) return
        started = true
        viewModelScope.launch { useLocationOrDefault(locationGranted) }
    }

    fun useMyLocation() = viewModelScope.launch { useLocationOrDefault(true) }

    private suspend fun useLocationOrDefault(granted: Boolean) {
        loading = true
        val loc = if (granted) runCatching { LocationHelper.current(getApplication()) }.getOrNull() else null
        val city = if (loc != null) WeatherApi.reverseGeocode(loc.latitude, loc.longitude)
        else prefs.city ?: City("İstanbul", 41.0082, 28.9784, "Türkiye")
        load(city)
    }

    fun refresh() {
        weather?.city?.let { c -> viewModelScope.launch { load(c) } }
    }

    private suspend fun load(city: City) {
        loading = true
        error = null
        runCatching { WeatherApi.forecast(city) }
            .onSuccess {
                weather = it
                prefs.city = city
                prefs.saveSnapshot(it)
                WeatherWidgetProvider.refreshAll(getApplication())
            }
            .onFailure { error = "Hava durumu alınamadı. İnternet bağlantını kontrol et." }
        loading = false
    }

    // ---------- Şehir arama ----------

    fun onQueryChange(q: String) {
        query = q
        searchJob?.cancel()
        if (q.trim().length < 2) {
            suggestions = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            suggestions = runCatching { WeatherApi.searchCity(q.trim()) }.getOrDefault(emptyList())
        }
    }

    fun selectCity(city: City) {
        query = ""
        suggestions = emptyList()
        viewModelScope.launch { load(city) }
    }

    fun clearSearch() {
        query = ""
        suggestions = emptyList()
    }

    // ---------- Paylaşılan anlar (Firestore, canlı) ----------

    private fun startFeed() {
        feed?.remove()
        feed = Cloud.listenFeed(
            onChange = { list ->
                moments = list
                viewModelScope.launch {
                    val changed = withContext(Dispatchers.IO) { widgetPhoto.update(list, Cloud.uid) }
                    if (changed) WeatherWidgetProvider.refreshAll(getApplication())
                }
            },
            onError = { message = it },
        )
    }

    fun addMoment(uri: Uri) {
        if (uploading) return
        viewModelScope.launch {
            uploading = true
            val result = runCatching {
                val base64 = withContext(Dispatchers.IO) { Images.encodeForUpload(getApplication(), uri) }
                Cloud.share(base64, weather)
            }
            uploading = false
            message = if (result.isSuccess) "Anın herkesle paylaşıldı ✨" else "Paylaşılamadı: ${result.exceptionOrNull()?.localizedMessage}"
        }
    }

    fun deleteMoment(m: Moment) {
        viewModelScope.launch {
            message = runCatching { Cloud.delete(m.id) }
                .fold({ "Paylaşım silindi" }, { "Silinemedi: ${it.localizedMessage}" })
        }
    }
}
