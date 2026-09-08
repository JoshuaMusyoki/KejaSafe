package com.rotd.kejasafe

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rotd.kejasafe.components.homeDestinationForRole
import com.rotd.kejasafe.navigation.KejaSafeDestination
import com.rotd.kejasafe.navigation.KejaSafeNavHost
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.theme.KejaSafeTheme
import com.rotd.kejasafe.theme.ThemeController
import com.rotd.kejasafe.theme.resolveDarkTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinContext

@Composable
@Preview
fun App() {
    // Do not SessionManager.restore() — Android clears session on process start so
    // killing the app logs the user out. Theme still persists.
    ThemeController.restore()

    val themeMode by ThemeController.mode.collectAsState()
    val darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme())
    val startDestination = when {
        SessionManager.isLogged() ->
            homeDestinationForRole(SessionManager.currentUser.value?.role)
        getPlatform().isWeb -> KejaSafeDestination.Landing
        else -> KejaSafeDestination.Login
    }

    KoinContext {
        KejaSafeTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                KejaSafeNavHost(startDestination = startDestination)
            }
        }
    }
}
