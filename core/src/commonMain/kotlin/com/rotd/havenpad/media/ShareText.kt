package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import com.rotd.havenpad.PropertyListing

@Composable
expect fun rememberListingImageShareHandler(): (listing: PropertyListing) -> Unit
