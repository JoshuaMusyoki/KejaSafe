package com.havenpad

import com.havenpad.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Room + KSP for KMP libraries (androidMain). Other targets use JsonCache.
 */
class KotlinMultiplatformRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.androidMain.dependencies {
                    implementation(libs.findLibrary("androidxRoomRuntime").get())
                    implementation(libs.findLibrary("androidxRoomKtx").get())
                }
            }

            dependencies {
                add("kspAndroid", libs.findLibrary("androidxRoomCompiler").get())
            }
        }
    }
}
