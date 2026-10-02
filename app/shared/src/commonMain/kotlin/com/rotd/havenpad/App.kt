package com.rotd.havenpad

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rotd.havenpad.components.homeDestinationForRole
import com.rotd.havenpad.navigation.HavenPadDestination
import com.rotd.havenpad.navigation.HavenPadNavHost
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ThemeController
import com.rotd.havenpad.theme.resolveDarkTheme
import com.rotd.havenpad.theme.ScreenPreviews
import org.koin.compose.KoinContext

@Composable
@ScreenPreviews
fun App() {
    // Do not SessionManager.restore() — Android clears session on process start so
    // killing the app logs the user out. Theme still persists.
    ThemeController.restore()

    val themeMode by ThemeController.mode.collectAsState()
    val darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme())
    val startDestination = when {
        SessionManager.isLogged() ->
            homeDestinationForRole(SessionManager.currentUser.value?.role)
        getPlatform().isWeb -> HavenPadDestination.Landing
        else -> HavenPadDestination.Login
    }

    KoinContext {
        HavenPadTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                HavenPadNavHost(startDestination = startDestination)
            }
        }
    }
}
