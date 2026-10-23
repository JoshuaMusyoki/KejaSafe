package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rotd.havenpad.PropertyListing

@Composable
actual fun rememberListingImageShareHandler(): (listing: PropertyListing) -> Unit {
    return remember {
        { listing ->
            // no-op
        }
    }
}
