package com.rotd.havenpad

import androidx.compose.ui.window.ComposeUIViewController
import com.rotd.havenpad.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}
