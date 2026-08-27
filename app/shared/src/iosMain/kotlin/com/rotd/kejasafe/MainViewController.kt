package com.rotd.kejasafe

import androidx.compose.ui.window.ComposeUIViewController
import com.rotd.kejasafe.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}
