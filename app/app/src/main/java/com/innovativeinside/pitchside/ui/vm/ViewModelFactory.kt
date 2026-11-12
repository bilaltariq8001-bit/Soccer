package com.innovativeinside.pitchside.ui.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innovativeinside.pitchside.AppContainer
import com.innovativeinside.pitchside.PitchsideApp

/** Builds a ViewModel from the app container plus the SavedStateHandle (which carries nav arguments). */
inline fun <reified VM : ViewModel> containerFactory(crossinline create: (AppContainer, SavedStateHandle) -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as PitchsideApp
            create(app.container, createSavedStateHandle())
        }
    }
