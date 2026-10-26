package com.example.havadurumuapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ekran 1 (Giriş Yap) ve Ekran 2 (Hesap Oluştur). */
@Composable
fun AuthScreen(
    onLogin: suspend (String, String) -> String?,
    onRegister: suspend (String, String) -> String?,
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }

    GalaxyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(120.dp))
            Text(
                "MelWeather",
                style = TextStyle(
                    color = Snow,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    shadow = Shadow(Rose, blurRadius = 28f),
                ),
            )
            Spacer(Modifier.height(22.dp))
            AnimatedContent(
                targetState = registerMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "auth",
            ) { isRegister ->
                AuthCard(
                    isRegister = isRegister,
                    onSubmit = { u, p -> if (isRegister) onRegister(u, p) else onLogin(u, p) },
                    onSwitch = { registerMode = !registerMode },
                )
            }
            Spacer(Modifier.height(160.dp))
        }
    }
}

@Composable
private fun AuthCard(
    isRegister: Boolean,
    onSubmit: suspend (String, String) -> String?,
    onSwitch: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val title = if (isRegister) "Hesap Oluştur" else "Giriş Yap"
    val submit = {
        if (!busy) {
            focus.clearFocus()
            scope.launch {
                busy = true
                error = onSubmit(username, password)
                busy = false
            }
        }
    }

    Column(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Snow, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        PillField(username, { username = it; error = null }, "Kullanıcı Adı", tag = "username")
        Spacer(Modifier.height(12.dp))
        PillField(
            password, { password = it; error = null }, "Şifre", tag = "password",
            isPassword = true, onDone = { submit() },
        )
        Spacer(Modifier.height(12.dp))
        error?.let {
            Text(it, color = Color(0xFFFFB4BE), fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            buildAnnotatedString {
                append(if (isRegister) "Hesabın var mı? " else "Hesabın yok mu? ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                    append(if (isRegister) "Hemen giriş yap." else "Hemen bir tane oluştur.")
                }
            },
            color = Snow.copy(alpha = .75f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(onClick = onSwitch).padding(4.dp).testTag("auth_switch"),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Mauve.copy(alpha = .75f))
                .border(1.dp, Snow.copy(alpha = .25f), RoundedCornerShape(50))
                .clickable { submit() }
                .padding(horizontal = 26.dp, vertical = 9.dp)
                .testTag("auth_button"),
        ) {
            if (busy) CircularProgressIndicator(color = Snow, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            else Text(title, color = Snow.copy(alpha = .9f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun PillField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    tag: String,
    isPassword: Boolean = false,
    onDone: () -> Unit = {},
) {
    val shape = RoundedCornerShape(50)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Snow, fontSize = 14.sp),
        cursorBrush = SolidColor(Rose),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = if (isPassword) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .background(Mauve.copy(alpha = .6f))
            .border(1.dp, Snow.copy(alpha = .12f), shape)
            .testTag(tag),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Snow.copy(alpha = .45f), fontSize = 13.sp)
                inner()
            }
        },
    )
}
