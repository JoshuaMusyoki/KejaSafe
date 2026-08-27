package com.kejasafe

import com.android.build.api.dsl.LibraryExtension
import com.kejasafe.convention.configureAndroidCompose
import com.kejasafe.convention.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure(LibraryExtension::class.java) {
                configureKotlinAndroid(this)
                configureAndroidCompose(this)
            }
        }
    }
}

