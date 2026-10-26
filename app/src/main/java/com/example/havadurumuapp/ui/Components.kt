package com.example.havadurumuapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.havadurumuapp.data.WeatherKind
import kotlin.random.Random

// ---- App color palette (tasarımdaki 6 renk) ----
val Ink = Color(0xFF0D090B)
val Coal = Color(0xFF2C2325)
val Mauve = Color(0xFF544547)
val Dusty = Color(0xFF826A6E)
val Rose = Color(0xFFC09CA3)
val Snow = Color(0xFFFCFBFB)

private class Star(val x: Float, val y: Float, val r: Float, val a: Float)

/** Pembe bulutsulu, yıldızlı galaksi arka planı. */
@Composable
fun GalaxyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val stars = remember {
        val rnd = Random(42)
        List(320) { Star(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 1.3f + .3f, rnd.nextFloat() * .7f + .2f) }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(Ink)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF151012), Color(0xFF1C1416), Ink)))
                nebula(Offset(size.width * .45f, size.height * .2f), size.width * .8f, Rose.copy(alpha = .6f), -28f, 1.9f)
                nebula(Offset(size.width * .5f, size.height * .22f), size.width * .35f, Color(0xFFF2D3D9).copy(alpha = .35f), -28f, 2f)
                nebula(Offset(size.width * .2f, size.height * .1f), size.width * .5f, Dusty.copy(alpha = .4f), 0f, 1f)
                nebula(Offset(size.width * .85f, size.height * .06f), size.width * .45f, Ink.copy(alpha = .9f), 0f, 1.2f)
                nebula(Offset(size.width * .55f, size.height * .5f), size.width * .6f, Dusty.copy(alpha = .35f), 20f, 1.6f)
                nebula(Offset(size.width * .6f, size.height * .8f), size.width * .75f, Rose.copy(alpha = .55f), -32f, 2.2f)
                nebula(Offset(size.width * .58f, size.height * .82f), size.width * .3f, Color(0xFFF2D3D9).copy(alpha = .3f), -32f, 2.2f)
                nebula(Offset(size.width * .3f, size.height * .95f), size.width * .5f, Ink.copy(alpha = .85f), 0f, 1.4f)
                stars.forEach { s ->
                    drawCircle(Snow.copy(alpha = s.a), radius = s.r * density, center = Offset(s.x * size.width, s.y * size.height))
                }
            },
        content = content,
    )
}

private fun DrawScope.nebula(center: Offset, radius: Float, color: Color, angle: Float, stretch: Float) {
    rotate(angle, center) {
        scale(stretch, 1f / stretch.coerceAtLeast(1f) * 1.1f, center) {
            drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius), radius, center)
        }
    }
}

/** Buzlu cam görünümü: yarı saydam pembe dolgu + parlak kenar. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(26.dp), strength: Float = 1f): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(Rose.copy(alpha = .30f * strength), Dusty.copy(alpha = .20f * strength), Rose.copy(alpha = .16f * strength))
        )
    )
    .border(
        1.dp,
        Brush.linearGradient(listOf(Snow.copy(alpha = .45f), Snow.copy(alpha = .06f), Snow.copy(alpha = .22f))),
        shape,
    )

@Composable
fun WeatherIcon(kind: WeatherKind, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawIntoCanvas { WeatherArt.draw(it.nativeCanvas, kind, isDay, size.width, size.height) }
    }
}

@Composable
fun GlassCircleButton(icon: ImageVector, contentDescription: String, modifier: Modifier = Modifier, size: Dp = 40.dp, onClick: () -> Unit) {
    Box(
        modifier
            .size(size)
            .glass(CircleShape, 1.3f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Snow.copy(alpha = .9f), modifier = Modifier.size(size * .5f))
    }
}

/** Gri-gümüş arama çubuğu (tasarımdaki gibi). */
@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = { onValueChange("") },
) {
    val shape = RoundedCornerShape(50)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
        cursorBrush = SolidColor(Coal),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(),
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFD9D4D5), Color(0xFFA9A3A5), Color(0xFF8E888A))))
            .border(1.dp, Snow.copy(alpha = .5f), shape)
            .testTag("search_field"),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Snow.copy(alpha = .75f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Search, null, tint = Mauve, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = Ink.copy(alpha = .45f), fontSize = 13.sp)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, "Temizle", tint = Coal,
                        modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onClear),
                    )
                }
            }
        },
    )
}

/** Sağ üstteki profil yuvarlağı; tıklanınca kullanıcı adı ve çıkış menüsü. */
@Composable
fun Avatar(user: String?, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFF4C7CF), Rose, Dusty)))
                .border(2.dp, Snow.copy(alpha = .7f), CircleShape)
                .clickable { open = true }
                .testTag("avatar"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                user?.firstOrNull()?.uppercase() ?: "?",
                color = Coal, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            )
        }
        DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.background(Coal)) {
            DropdownMenuItem(text = { Text("@${user.orEmpty()}", color = Rose) }, onClick = { open = false }, enabled = false)
            DropdownMenuItem(text = { Text("Çıkış Yap", color = Snow) }, onClick = { open = false; onLogout() })
        }
    }
}
