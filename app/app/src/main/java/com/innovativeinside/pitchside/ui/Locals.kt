package com.innovativeinside.pitchside.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.ui.vm.AppViewModel

/** The activity-scoped [AppViewModel]: follows, Spoiler Shield and settings intents. */
val LocalApp = staticCompositionLocalOf<AppViewModel> { error("AppViewModel not provided") }

/** The current preferences snapshot. Always loaded by the time any screen composes. */
val LocalPrefs = staticCompositionLocalOf { UserPrefs() }
