package com.example.havadurumuapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Hocanın önerdiği açık kaynaklar:
 *  - Open-Meteo        → hava tahmini + şehir arama (geocoding)
 *  - OpenStreetMap     → konumdan şehir adı bulma (Nominatim reverse geocoding)
 * İkisi de API anahtarı istemez.
 */
object WeatherApi {

    private const val USER_AGENT = "MelWeather/1.0 (Android student project)"

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/json")
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    suspend fun forecast(city: City): Weather = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day" +
            "&hourly=temperature_2m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=7"
        val root = JSONObject(get(url))
        val offset = ZoneOffset.ofTotalSeconds(root.optInt("utc_offset_seconds", 0))

        val cur = root.getJSONObject("current")
        val now = LocalDateTime.parse(cur.getString("time"))

        val h = root.getJSONObject("hourly")
        val hTimes = h.getJSONArray("time")
        val hTemps = h.getJSONArray("temperature_2m")
        val hCodes = h.getJSONArray("weather_code")
        val hDay = h.getJSONArray("is_day")
        val currentHour = now.withMinute(0)
        val hourly = (0 until hTimes.length())
            .map { i ->
                HourItem(
                    time = LocalDateTime.parse(hTimes.getString(i)),
                    temp = hTemps.optDouble(i),
                    code = hCodes.optInt(i),
                    isDay = hDay.optInt(i) == 1,
                )
            }
            .filter { !it.time.isBefore(currentHour) }
            .take(24)

        val d = root.getJSONObject("daily")
        val dTimes = d.getJSONArray("time")
        val dMax = d.getJSONArray("temperature_2m_max")
        val dMin = d.getJSONArray("temperature_2m_min")
        val dCodes = d.getJSONArray("weather_code")
        val daily = (0 until dTimes.length()).map { i ->
            DayItem(LocalDate.parse(dTimes.getString(i)), dMax.optDouble(i), dMin.optDouble(i), dCodes.optInt(i))
        }

        Weather(
            city = city,
            offset = offset,
            temp = cur.optDouble("temperature_2m"),
            feelsLike = cur.optDouble("apparent_temperature"),
            humidity = cur.optInt("relative_humidity_2m"),
            wind = cur.optDouble("wind_speed_10m"),
            code = cur.optInt("weather_code"),
            isDay = cur.optInt("is_day", 1) == 1,
            hourly = hourly,
            daily = daily,
        )
    }

    suspend fun searchCity(query: String): List<City> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${enc(query)}&count=8&language=tr&format=json"
        val results = JSONObject(get(url)).optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            City(
                name = o.getString("name"),
                lat = o.getDouble("latitude"),
                lon = o.getDouble("longitude"),
                region = listOfNotNull(o.optString("admin1").ifBlank { null }, o.optString("country").ifBlank { null })
                    .joinToString(", "),
            )
        }
    }

    /** OpenStreetMap Nominatim ile koordinattan şehir adı. */
    suspend fun reverseGeocode(lat: Double, lon: Double): City = withContext(Dispatchers.IO) {
        val name = runCatching {
            val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon&zoom=10&accept-language=tr"
            val addr = JSONObject(get(url)).getJSONObject("address")
            listOf("city", "town", "province", "village", "county", "state")
                .firstNotNullOfOrNull { addr.optString(it).ifBlank { null } }
        }.getOrNull()
        City(name ?: "Konumum", lat, lon)
    }
}
