package com.rotd.havenpad.media

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
internal fun topViewController(): UIViewController? {
    val application = UIApplication.sharedApplication
    val windows = application.windows.mapNotNull { it as? UIWindow }
    val window = windows.firstOrNull { it.isKeyWindow }
        ?: application.keyWindow
        ?: windows.firstOrNull()
    var controller = window?.rootViewController ?: return null
    while (true) {
        val presented = controller.presentedViewController ?: break
        controller = presented
    }
    return controller
}

internal fun present(controller: UIViewController) {
    topViewController()?.presentViewController(controller, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
internal fun writeSharedFile(fileName: String, bytes: ByteArray): NSURL? {
    val root = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String
        ?: return null
    val folder = "$root/HavenPad"
    NSFileManager.defaultManager.createDirectoryAtPath(
        folder,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    val safe = fileName.safeFileName()
    val path = "$folder/$safe"
    if (!bytes.toNsData().writeToFile(path, atomically = true)) return null
    return NSURL.fileURLWithPath(path)
}

internal fun String.safeFileName(): String {
    val cleaned = replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').take(80)
    return cleaned.ifBlank { "havenpad" }
}
