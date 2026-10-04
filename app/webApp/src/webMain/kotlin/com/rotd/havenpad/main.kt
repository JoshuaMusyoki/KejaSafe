package com.rotd.havenpad

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.rotd.havenpad.di.initKoin
import com.rotd.havenpad.network.SessionManager

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    SessionManager.dropSavedSession()
    ComposeViewport("compose-target") {
        App()
    }
}
