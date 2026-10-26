package com.example.havadurumuapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.havadurumuapp.HomeViewModel
import com.example.havadurumuapp.data.Images
import com.example.havadurumuapp.data.Moment
import com.example.havadurumuapp.data.weatherKind
import com.example.havadurumuapp.data.weatherLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val TimeFmt = SimpleDateFormat("HH:mm", Locale("tr"))
private val DateFmt = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("tr"))

/** Ekran 5: Shared moments (paylaşılan anlar). */
@Composable
fun MomentsScreen(vm: HomeViewModel, onBack: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    val list = vm.moments.filter {
        filter.isBlank() || it.city.contains(filter, true) || it.user.contains(filter, true)
    }

    GalaxyBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                GlassCircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Geri", Modifier.testTag("back_button"), onClick = onBack)
                Spacer(Modifier.width(10.dp))
                SearchPill(filter, { filter = it }, "Şehir veya kullanıcı ara…", Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Avatar(vm.user, vm::logout)
            }

            if (list.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, null, tint = Snow.copy(alpha = .6f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (vm.moments.isEmpty()) "Henüz paylaşılan an yok.\nAna ekrandaki + butonuyla fotoğraf paylaş."
                        else "Aramana uygun an bulunamadı.",
                        color = Snow.copy(alpha = .7f), textAlign = TextAlign.Center, fontSize = 13.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.navigationBarsPadding().testTag("moments_grid"),
                ) {
                    items(list, key = { it.id }) { m -> MomentCard(m) { selected = m.id } }
                }
            }
        }
    }

    vm.moments.firstOrNull { it.id == selected }?.let { m ->
        Dialog(onDismissRequest = { selected = null }) {
            Column(
                Modifier.glass(RoundedCornerShape(26.dp), 2.2f).background(Coal.copy(alpha = .6f)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MomentImage(m, Modifier.fillMaxWidth().aspectRatio(.8f).clip(RoundedCornerShape(18.dp)), req = 1080)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIcon(weatherKind(m.code), m.isDay, Modifier.size(34.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${m.city} · ${m.temp.roundToInt()}°C · ${weatherLabel(m.code)}", color = Snow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("@${m.user} · ${DateFmt.format(Date(m.time))}", color = Snow.copy(alpha = .7f), fontSize = 12.sp)
                    }
                    if (m.userId == vm.uid) {
                        GlassCircleButton(Icons.Outlined.Delete, "Sil", Modifier.testTag("delete_moment"), size = 36.dp) {
                            selected = null
                            vm.deleteMoment(m)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentCard(m: Moment, onClick: () -> Unit) {
    Column(
        Modifier
            .glass(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(7.dp)
    ) {
        MomentImage(m, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.padding(horizontal = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Caption(m.user)
                Caption(m.city, dim = true)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Caption("${weatherLabel(m.code)} ${m.temp.roundToInt()}°")
                Caption(TimeFmt.format(Date(m.time)), dim = true)
            }
        }
    }
}

@Composable
private fun Caption(text: String, dim: Boolean = false) {
    Text(
        text,
        color = if (dim) Snow.copy(alpha = .65f) else Snow,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun MomentImage(m: Moment, modifier: Modifier = Modifier, req: Int = 600) {
    val bitmap by produceState<ImageBitmap?>(null, m.id, req) {
        value = withContext(Dispatchers.IO) { Images.fromBase64(m.id, m.imageBase64, req)?.asImageBitmap() }
    }
    Box(modifier.background(Coal)) {
        bitmap?.let {
            Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
