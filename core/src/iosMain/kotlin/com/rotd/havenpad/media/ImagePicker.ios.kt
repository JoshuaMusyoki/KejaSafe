package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rotd.havenpad.PickedImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImage>) -> Unit
): () -> Unit {
    val onPicked by rememberUpdatedState(onImagesPicked)
    return remember {
        { presentPhotoPicker { onPicked(it) } }
    }
}

private var photoPickerDelegate: NSObject? = null

@OptIn(ExperimentalForeignApi::class)
private fun presentPhotoPicker(onImagesPicked: (List<PickedImage>) -> Unit) {
    val configuration = PHPickerConfiguration()
    configuration.selectionLimit = 0
    configuration.filter = PHPickerFilter.imagesFilter
    val picker = PHPickerViewController(configuration = configuration)
    val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            picker.dismissViewControllerAnimated(true, null)
            val results = didFinishPicking.mapNotNull { it as? PHPickerResult }
            if (results.isEmpty()) {
                photoPickerDelegate = null
                return
            }
            loadPhotos(results, 0, mutableListOf()) { images ->
                photoPickerDelegate = null
                if (images.isNotEmpty()) {
                    dispatch_async(dispatch_get_main_queue()) {
                        onImagesPicked(images)
                    }
                }
            }
        }
    }
    photoPickerDelegate = delegate
    picker.delegate = delegate
    present(picker)
}

private fun loadPhotos(
    results: List<PHPickerResult>,
    index: Int,
    loaded: MutableList<PickedImage>,
    done: (List<PickedImage>) -> Unit,
) {
    if (index >= results.size) {
        done(loaded.toList())
        return
    }
    val provider = results[index].itemProvider
    val type = provider.bestImageType()
    provider.loadDataRepresentationForTypeIdentifier(type) { data, _ ->
        data?.toJpegPhoto(provider.suggestedName ?: "photo")?.let { loaded.add(it) }
        loadPhotos(results, index + 1, loaded, done)
    }
}

private fun NSItemProvider.bestImageType(): String {
    val candidates = listOf("public.jpeg", "public.png", "public.heic", "public.heif", "public.image")
    return candidates.firstOrNull { hasItemConformingToTypeIdentifier(it) } ?: "public.image"
}

@OptIn(ExperimentalForeignApi::class)
private fun platform.Foundation.NSData.toJpegPhoto(suggestedName: String): PickedImage? {
    val image = UIImage.imageWithData(this) ?: return null
    val jpeg = UIImageJPEGRepresentation(image, 0.82) ?: return null
    val bytes = jpeg.toByteArray()
    if (bytes.isEmpty()) return null
    val base = suggestedName.substringBeforeLast('.').safeFileName()
    return PickedImage(fileName = "$base.jpg", bytes = bytes, mimeType = "image/jpeg")
}
