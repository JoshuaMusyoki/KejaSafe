package com.rotd.keja_safe.domain.util

/**
 * Play In-App Updates entry point. Implemented in the app module so Feature/UI layers
 * can request a check without depending on Play Core.
 */
interface AppUpdateGateway {
    /**
     * Check Play Store for a newer version and start download/install when available.
     * @param preferImmediate when true (e.g. user tapped a release notification), prefer a blocking update.
     */
    fun checkForUpdate(preferImmediate: Boolean = false)
}
