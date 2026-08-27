package com.kejasafe.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Configure base Kotlin options for Android
 */
internal fun Project.configureKotlinAndroid(
    extension: CommonExtension,
) {
    val compileSdkVersion = libs.findVersion("androidCompileSdk").get().requiredVersion.toInt()
    val minSdk = libs.findVersion("androidMinSdk").get().requiredVersion.toInt()

    when (extension) {
        is ApplicationExtension -> {
            extension.compileSdk = compileSdkVersion
            extension.defaultConfig.minSdk = minSdk
        }
        is LibraryExtension -> {
            extension.compileSdk = compileSdkVersion
            extension.defaultConfig.minSdk = minSdk
        }
    }

    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_11
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_11
    extension.compileOptions.isCoreLibraryDesugaringEnabled = false

    configureKotlin<KotlinAndroidProjectExtension>()

    dependencies {
        add("implementation", libs.findLibrary("androidxCoreKtx").get())
        add("implementation", libs.findLibrary("firebaseMessaging").get())
        // Do not add google-api-client here — it pulls grpc-api 1.70 and crashes
        // Firebase Data Connect (grpc-core 1.62) with NoClassDefFoundError:
        // InternalGlobalInterceptors. Server JWT verify keeps googleAuth separately.

        add("coreLibraryDesugaring", libs.findLibrary("desugarJdkLibs").get())
        add("androidTestImplementation", libs.findLibrary("androidxEspressoCore").get())
        add("androidTestImplementation", libs.findLibrary("androidxJunit").get())
        add("androidTestImplementation", libs.findLibrary("testJunitKtx").get())
    }
}

/**
 * Configure base Kotlin options for JVM (non-Android)
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

/**
 * Configure base Kotlin options
 */
private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    val warningsAsErrors: String? by project
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> TODO("Unsupported project extension $this ${T::class}")
    }.apply {
        jvmTarget = JvmTarget.JVM_11
        allWarningsAsErrors = warningsAsErrors.toBoolean()
        freeCompilerArgs.add(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}