package com.rotd.havenpad.theme

import androidx.compose.ui.tooling.preview.Preview

/**
 * Reusable multi-preview for all screen sizes (Mobile, Tablet, Desktop) in both Light and Dark modes.
 * Desktop size (1440dp) will trigger the 'wide' adaptive layout in the HavenPad web components.
 */
@Preview(name = "Mobile Light", group = "Mobile", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Mobile Dark", group = "Mobile", widthDp = 360, heightDp = 640, showBackground = true, uiMode = 32)
@Preview(name = "Tablet Light", group = "Tablet", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Tablet Dark", group = "Tablet", widthDp = 800, heightDp = 1280, showBackground = true, uiMode = 32)
@Preview(name = "Desktop Light", group = "Desktop", widthDp = 1440, heightDp = 900, showBackground = true)
@Preview(name = "Desktop Dark", group = "Desktop", widthDp = 1440, heightDp = 900, showBackground = true, uiMode = 32)
annotation class ScreenPreviews

/**
 * Specifically for Web-focused components that primarily target wide screens.
 */
@Preview(name = "Web Light", group = "Web", widthDp = 1280, heightDp = 800, showBackground = true)
@Preview(name = "Web Dark", group = "Web", widthDp = 1280, heightDp = 800, showBackground = true, uiMode = 32)
annotation class WebPreviews

/**
 * Device-only previews without theme variation.
 */
@Preview(name = "Mobile", group = "Device", widthDp = 360, heightDp = 640)
@Preview(name = "Tablet", group = "Device", widthDp = 800, heightDp = 1280)
@Preview(name = "Desktop", group = "Device", widthDp = 1440, heightDp = 900)
annotation class DevicePreviews
