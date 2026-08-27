package com.rotd.kejasafe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KejaSafeBottomNavBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.material3.NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        content = content
    )
}

/**
 * Frosted, always-on header — place as an overlay at the top of a [Box]
 * so scroll content can pass underneath it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KejaSafeHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    frosted: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val surface = MaterialTheme.colorScheme.surface
    val container = if (frosted) Color.Transparent else surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (frosted) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                surface.copy(alpha = 0.94f),
                                surface.copy(alpha = 0.82f),
                                surface.copy(alpha = 0.0f)
                            )
                        )
                    )
                } else {
                    Modifier.background(surface)
                }
            )
            .statusBarsPadding()
    ) {
        // Soft blur layer for glass effect (tints blend with scrolling content behind).
        if (frosted) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(18.dp)
                    .background(surface.copy(alpha = 0.35f))
            )
        }
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = container,
                scrolledContainerColor = container,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

/** Approximate space to reserve so the first line of content isn’t under the frosted header. */
val KejaSafeHeaderContentInset = 72.dp
