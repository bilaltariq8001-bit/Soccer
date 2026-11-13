package com.innovativeinside.pitchside

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovativeinside.pitchside.data.prefs.ThemeChoice
import com.innovativeinside.pitchside.ui.AppRoot
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.screens.AnimatedSplashScreen
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PitchsideTheme
import com.innovativeinside.pitchside.ui.vm.AppViewModel

class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash until preferences are read, so onboarding never flashes for returning users.
        installSplashScreen().setKeepOnScreenCondition { app.prefs.value == null }
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by app.prefs.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (prefs?.theme) { ThemeChoice.Light -> false; ThemeChoice.Dark -> true; else -> systemDark }
            // A fresh launch gets the short brand intro; recreation and returning from background do not replay it.
            var showIntro by remember { mutableStateOf(savedInstanceState == null && intent?.data == null) }
            DisposableEffect(dark, showIntro) {
                val style = if (dark || showIntro) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            PitchsideTheme(dark) {
                val p = prefs
                Box(Modifier.fillMaxSize().background(Pc.Bg)) {
                    if (p != null) CompositionLocalProvider(LocalApp provides app, LocalPrefs provides p) { AppRoot(app, p) }
                    // The intro overlays the already-composed app, so it never delays loading.
                    if (showIntro) AnimatedSplashScreen(onFinished = { showIntro = false })
                }
            }
        }
    }
}
