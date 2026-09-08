package com.rotd.kejasafe.data

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun platformEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
