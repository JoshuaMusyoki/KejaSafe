package com.rotd.havenpad.media

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.toSharePosterText

@Composable
actual fun rememberListingImageShareHandler(): (listing: PropertyListing) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { listing ->
            val shareText = buildString {
                append(listing.toSharePosterText())
                val imgUrl = listing.imageUrl.takeIf { !it.isNullOrBlank() }
                if (imgUrl != null) {
                    append("\n\nPhoto: $imgUrl")
                }
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Listing")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }
}
