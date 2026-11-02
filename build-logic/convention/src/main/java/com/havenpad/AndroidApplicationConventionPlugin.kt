package com.havenpad

import com.havenpad.convention.configureKotlinAndroid
import com.havenpad.convention.libs
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure(ApplicationExtension::class.java) {
                configureKotlinAndroid(this)
                defaultConfig {
                    targetSdk = libs.findVersion("androidTargetSdk").get().requiredVersion.toInt()
                }
                
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
            }
        }
    }
}
