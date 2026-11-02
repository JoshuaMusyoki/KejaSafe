package com.havenpad.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlinMultiplatform(
    extension: KotlinMultiplatformExtension
) {
    extension.apply {
        // Use reflection to check if android target exists and call androidTarget() if not
        try {
            val targets = this::class.java.getMethod("getTargets").invoke(this) as org.gradle.api.NamedDomainObjectContainer<*>
            if (targets.findByName("android") == null) {
                try {
                    this::class.java.getMethod("android").invoke(this)
                } catch (e: Exception) {
                    this::class.java.getMethod("androidTarget").invoke(this)
                }
            }
        } catch (e: Exception) {
            // Fallback: try to find the method and call it anyway if targets check failed
            try {
                val method = this::class.java.methods.find { it.name == "android" }
                    ?: this::class.java.methods.find { it.name == "androidTarget" }
                method?.invoke(this)
            } catch (e2: Exception) {}
        }
        
        iosArm64()
        iosSimulatorArm64()

        jvm()

        js {
            browser()
        }

        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser()
        }

        // Configure the Android target added by the com.android.kotlin.multiplatform.library plugin
        // Use the new declarative syntax for AGP 9.3.1
        val compileSdkValue = libs.findVersion("androidCompileSdk").get().requiredVersion.toInt()
        val minSdkValue = libs.findVersion("androidMinSdk").get().requiredVersion.toInt()

        (this as? ExtensionAware)?.extensions?.findByName("android")?.let { target ->
            if (target is KotlinMultiplatformAndroidLibraryTarget) {
                target.compileSdk {
                    version = release(compileSdkValue)
                }
                target.minSdk {
                    version = release(minSdkValue)
                }
            }
        }

        sourceSets.apply {
            commonMain.dependencies {
                implementation(libs.findLibrary("composeRuntime").get())
                implementation(libs.findLibrary("composeFoundation").get())
                implementation(libs.findLibrary("composeMaterial3").get())
                implementation(libs.findLibrary("composeUi").get())
                implementation(libs.findLibrary("composeComponentsResources").get())
                implementation(libs.findLibrary("composeUiToolingPreview").get())
                implementation(libs.findLibrary("jetbrainsComposeUiToolingPreview").get())
                implementation(libs.findLibrary("composeMaterialIconsExtended").get())
                implementation(libs.findLibrary("jetbrainsNavigation3Ui").get())
                implementation(libs.findLibrary("kotlinxSerializationJson").get())

                implementation(libs.findLibrary("ktorClientCore").get())
                implementation(libs.findLibrary("ktorClientContentNegotiation").get())
                implementation(libs.findLibrary("ktorClientLogging").get())
                implementation(libs.findLibrary("ktorSerializationKotlinxJson").get())

                implementation(libs.findLibrary("androidxLifecycleViewmodelCompose").get())
                implementation(libs.findLibrary("androidxLifecycleRuntimeCompose").get())
                implementation(libs.findLibrary("jetbrainsLifecycleViewmodel").get())

                implementation(libs.findLibrary("koinCore").get())
                implementation(libs.findLibrary("koinCoreViewModel").get())
                implementation(libs.findLibrary("koinCompose").get())
                implementation(libs.findLibrary("koinComposeViewmodel").get())
            }
            androidMain.dependencies {
                implementation(libs.findLibrary("ktorClientOkhttp").get())
                implementation(libs.findLibrary("androidxActivityCompose").get())
                implementation(libs.findLibrary("webRtc").get())
                implementation(libs.findLibrary("koinAndroid").get())
                implementation(libs.findLibrary("androidxComposeUiTooling").get())
            }
            iosMain.dependencies {
                implementation(libs.findLibrary("ktorClientDarwin").get())
            }
            commonTest.dependencies {
                implementation(libs.findLibrary("kotlinTest").get())
            }
        }
    }
}
