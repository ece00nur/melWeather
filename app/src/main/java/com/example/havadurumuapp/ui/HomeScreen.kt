package com.example.havadurumuapp.ui

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.havadurumuapp.HomeViewModel
import com.example.havadurumuapp.data.DayItem
import com.example.havadurumuapp.data.HourItem
import com.example.havadurumuapp.data.LocationHelper
import com.example.havadurumuapp.data.Weather
import com.example.havadurumuapp.data.weatherKind
import com.example.havadurumuapp.data.weatherLabel
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import java.time.format.TextStyle as DateStyle

private val HourFmt = DateTimeFormatter.ofPattern("HH:mm")

/** Ekran 3 / 4: ana hava durumu ekranı + speed dial FAB. */
@Composable
fun HomeScreen(vm: HomeViewModel, onOpenMoments: () -> Unit) {
    val context = LocalContext.current

    // Konum izni → ilk yükleme
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        vm.start(res.values.any { it })
    }
    LaunchedEffect(Unit) {
        if (LocationHelper.hasPermission(context)) vm.start(true)
        else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    LaunchedEffect(vm.message) {
        vm.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.message = null }
    }

    // Kamera & galeri
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { vm.addMoment(Uri.parse(it)) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::addMoment)
    }
    val openCamera = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri.toString()
        runCatching { takePicture.launch(uri) }
            .onFailure { Toast.makeText(context, "Kamera uygulaması bulunamadı", Toast.LENGTH_SHORT).show() }
        Unit
    }
    val openGallery = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    GalaxyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            // --- Üst bar: paylaşılanlar | şehir arama | profil ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassCircleButton(Icons.Outlined.FavoriteBorder, "Paylaşılan anlar", Modifier.testTag("moments_button"), onClick = onOpenMoments)
                Spacer(Modifier.width(10.dp))
                SearchPill(vm.query, vm::onQueryChange, "Şehir ara…", Modifier.weight(1f), onClear = vm::clearSearch)
                Spacer(Modifier.width(10.dp))
                Avatar(vm.user, vm::logout)
            }

            // --- Arama sonuçları ---
            AnimatedVisibility(vm.suggestions.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(18.dp), 1.4f)
                        .padding(vertical = 4.dp)
                ) {
                    vm.suggestions.forEach { c ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.selectCity(c) }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(c.name, color = Snow, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (c.subtitle.isNotBlank()) Text(c.subtitle, color = Snow.copy(alpha = .6f), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            MainCard(vm)
            Spacer(Modifier.height(12.dp))

            vm.weather?.let { DaysRow(it.daily) }
            Spacer(Modifier.height(12.dp))

            MomentsStrip(vm, onOpenMoments)
            Spacer(Modifier.height(110.dp))
        }

        SpeedDial(
            uploading = vm.uploading,
            onCamera = openCamera,
            onGallery = openGallery,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun MainCard(vm: HomeViewModel) {
    val w = vm.weather
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(26.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            w != null -> WeatherContent(w, vm)
            vm.error != null -> {
                Spacer(Modifier.height(40.dp))
                Text(vm.error!!, color = Snow, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(12.dp))
                GlassCircleButton(Icons.Outlined.Refresh, "Tekrar dene") { vm.useMyLocation() }
                Spacer(Modifier.height(40.dp))
            }
            else -> {
                Spacer(Modifier.height(120.dp))
                CircularProgressIndicator(color = Rose)
                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun WeatherContent(w: Weather, vm: HomeViewModel) {
    var now by remember(w.offset) { mutableStateOf(LocalDateTime.now(w.offset)) }
    LaunchedEffect(w.offset) {
        while (true) {
            now = LocalDateTime.now(w.offset)
            delay(15_000)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
        Icon(
            Icons.Outlined.MyLocation, "Konumumu kullan", tint = Snow.copy(alpha = .55f),
            modifier = Modifier.size(26.dp).clip(CircleShape).clickable { vm.useMyLocation() }.padding(4.dp),
        )
        Text(
            w.city.name,
            color = Snow.copy(alpha = .85f),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("city_name"),
        )
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            if (vm.loading) CircularProgressIndicator(color = Snow.copy(alpha = .6f), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            else Icon(
                Icons.Outlined.Refresh, "Yenile", tint = Snow.copy(alpha = .55f),
                modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { vm.refresh() }.padding(4.dp),
            )
        }
    }

    WeatherIcon(weatherKind(w.code), w.isDay, Modifier.size(width = 230.dp, height = 170.dp))

    Text(
        now.dayOfWeek.getDisplayName(DateStyle.FULL, Locale.ENGLISH).uppercase(),
        color = Snow, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
    )
    Text("${w.temp.roundToInt()} °C", color = Snow, fontSize = 34.sp, fontWeight = FontWeight.Black)
    Text(now.format(HourFmt), color = Snow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Text(
        "${weatherLabel(w.code)} · Hissedilen ${w.feelsLike.roundToInt()}° · Nem %${w.humidity} · Rüzgar ${w.wind.roundToInt()} km/sa",
        color = Snow.copy(alpha = .6f), fontSize = 11.sp, textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    Spacer(Modifier.height(10.dp))
    Text(
        "Hourly Forecast",
        color = Snow.copy(alpha = .6f), fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
    Spacer(Modifier.height(8.dp))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("hourly_list"),
    ) {
        items(w.hourly) { HourChip(it) }
    }
}

@Composable
private fun HourChip(h: HourItem) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Rose.copy(alpha = .75f), Dusty.copy(alpha = .85f))))
                .border(1.dp, Snow.copy(alpha = .8f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            WeatherIcon(weatherKind(h.code), h.isDay, Modifier.size(34.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(h.time.format(HourFmt), color = Snow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("${h.temp.roundToInt()}°", color = Snow.copy(alpha = .7f), fontSize = 10.sp)
    }
}

@Composable
private fun DaysRow(days: List<DayItem>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("daily_list")) {
        items(days) { d ->
            Column(
                Modifier
                    .width(80.dp)
                    .height(112.dp)
                    .glass(RoundedCornerShape(18.dp))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    d.date.dayOfWeek.getDisplayName(DateStyle.SHORT, Locale.ENGLISH).uppercase(),
                    color = Snow.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
                WeatherIcon(weatherKind(d.code), true, Modifier.size(38.dp))
                Text(
                    "${d.max.roundToInt()}° / ${d.min.roundToInt()}°",
                    color = Snow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MomentsStrip(vm: HomeViewModel, onOpenMoments: () -> Unit) {
    val moments = vm.moments
    if (moments.isEmpty()) {
        Column(
            Modifier
                .size(130.dp)
                .glass(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenMoments)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.PhotoCamera, null, tint = Snow.copy(alpha = .7f), modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text("İlk anını + ile paylaş", color = Snow.copy(alpha = .7f), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 72.dp)) {
        items(moments.take(10), key = { it.id }) { m ->
            MomentImage(
                m,
                Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Snow.copy(alpha = .25f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenMoments),
                req = 400,
            )
        }
    }
}

/** Speed Dial Floating Action Button: + açılınca kamera ve galeri. */
@Composable
private fun SpeedDial(uploading: Boolean, onCamera: () -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            open,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlassCircleButton(Icons.Outlined.PhotoCamera, "Fotoğraf çek", Modifier.testTag("fab_camera"), size = 38.dp) {
                    open = false; onCamera()
                }
                Spacer(Modifier.height(10.dp))
                GlassCircleButton(Icons.Outlined.Image, "Galeriden seç", Modifier.testTag("fab_gallery"), size = 38.dp) {
                    open = false; onGallery()
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Snow.copy(alpha = .55f), Dusty.copy(alpha = .55f))))
                .border(1.5.dp, Snow.copy(alpha = .7f), CircleShape)
                .clickable(enabled = !uploading) { open = !open }
                .testTag("fab_main"),
            contentAlignment = Alignment.Center,
        ) {
            if (uploading) {
                CircularProgressIndicator(color = Snow, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                return@Box
            }
            Crossfade(open, label = "fab") { isOpen ->
                Icon(
                    if (isOpen) Icons.Default.KeyboardArrowDown else Icons.Default.Add,
                    if (isOpen) "Kapat" else "Paylaş",
                    tint = Snow,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
