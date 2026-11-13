package com.innovativeinside.pitchside

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.innovativeinside.pitchside.ui.AppRoot
import com.innovativeinside.pitchside.ui.screens.AnimatedSplashScreen
import com.innovativeinside.pitchside.ui.theme.PitchsideTheme

class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            val dark = when (app.themeChoice) { "Light" -> false; "Dark" -> true; else -> systemDark }
            // A fresh process launch gets the intro; recreation and returning from background do not replay it.
            var showAnimatedSplash by remember {
                mutableStateOf(savedInstanceState == null)
            }
            // Status and navigation bar icons switch with the theme so they stay readable.
            DisposableEffect(dark, showAnimatedSplash) {
                val style = if (dark || showAnimatedSplash) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            PitchsideTheme(dark) {
                CompositionLocalProvider(LocalApp provides app) {
                    Box(Modifier.fillMaxSize()) {
                        AppRoot(app)
                        if (showAnimatedSplash) {
                            AnimatedSplashScreen(onFinished = { showAnimatedSplash = false })
                        }
                    }
                }
            }
        }
    }
}
