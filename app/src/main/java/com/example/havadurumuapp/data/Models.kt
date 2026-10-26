package com.example.havadurumuapp.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

data class City(
    val name: String,
    val lat: Double,
    val lon: Double,
    val region: String? = null,
) {
    val subtitle: String get() = region.orEmpty()
}

data class HourItem(val time: LocalDateTime, val temp: Double, val code: Int, val isDay: Boolean)

data class DayItem(val date: LocalDate, val max: Double, val min: Double, val code: Int)

data class Weather(
    val city: City,
    val offset: ZoneOffset,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val wind: Double,
    val code: Int,
    val isDay: Boolean,
    val hourly: List<HourItem>,
    val daily: List<DayItem>,
)

/** Paylaşılan an (fotoğraf + o anki hava durumu). Firestore'daki "posts" belgesi. */
data class Moment(
    val id: String,
    val userId: String,
    val user: String,
    val city: String,
    val temp: Double,
    val code: Int,
    val isDay: Boolean,
    val time: Long,
    val imageBase64: String,
)

enum class WeatherKind { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

/** WMO hava kodlarını sadeleştirir. */
fun weatherKind(code: Int): WeatherKind = when (code) {
    0 -> WeatherKind.CLEAR
    1, 2 -> WeatherKind.PARTLY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    51, 53, 55, 56, 57 -> WeatherKind.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
    95, 96, 99 -> WeatherKind.THUNDER
    else -> WeatherKind.CLOUDY
}

fun weatherLabel(code: Int): String = when (weatherKind(code)) {
    WeatherKind.CLEAR -> "Açık"
    WeatherKind.PARTLY -> "Parçalı bulutlu"
    WeatherKind.CLOUDY -> "Bulutlu"
    WeatherKind.FOG -> "Sisli"
    WeatherKind.DRIZZLE -> "Çiseleme"
    WeatherKind.RAIN -> "Yağmurlu"
    WeatherKind.SNOW -> "Karlı"
    WeatherKind.THUNDER -> "Fırtınalı"
}
