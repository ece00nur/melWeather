package com.example.havadurumuapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.havadurumuapp.ui.AuthScreen
import com.example.havadurumuapp.ui.Coal
import com.example.havadurumuapp.ui.HomeScreen
import com.example.havadurumuapp.ui.Ink
import com.example.havadurumuapp.ui.MomentsScreen
import com.example.havadurumuapp.ui.Rose
import com.example.havadurumuapp.ui.Snow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { MelWeatherApp() }
    }
}

private enum class Screen { Auth, Home, Moments }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MelWeatherApp(vm: HomeViewModel = viewModel()) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Rose, background = Ink, surface = Coal, onSurface = Snow)) {
        var showMoments by rememberSaveable { mutableStateOf(false) }
        val screen = when {
            vm.user == null -> Screen.Auth
            showMoments -> Screen.Moments
            else -> Screen.Home
        }
        LaunchedEffect(vm.user) { if (vm.user == null) showMoments = false }
        BackHandler(enabled = screen == Screen.Moments) { showMoments = false }

        // testTagsAsResourceId: Maestro testleri test etiketlerini id olarak görebilsin
        Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { s ->
                when (s) {
                    Screen.Auth -> AuthScreen(onLogin = vm::login, onRegister = vm::register)
                    Screen.Home -> HomeScreen(vm, onOpenMoments = { showMoments = true })
                    Screen.Moments -> MomentsScreen(vm, onBack = { showMoments = false })
                }
            }
        }
    }
}
