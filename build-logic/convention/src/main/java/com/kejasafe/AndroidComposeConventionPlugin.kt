package com.kejasafe

import com.android.build.api.dsl.CommonExtension
import com.kejasafe.convention.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            @Suppress("UNCHECKED_CAST")
            val extension = extensions.findByType(CommonExtension::class.java)
            if (extension != null) {
                configureAndroidCompose(extension)
            }
        }
    }
}