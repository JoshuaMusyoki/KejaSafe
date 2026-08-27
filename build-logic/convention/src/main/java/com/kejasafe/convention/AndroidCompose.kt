package com.kejasafe.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/**
 * Configure Compose options for Android
 */
internal fun Project.configureAndroidCompose(
    extension: CommonExtension
) {
    extension.apply {
        buildFeatures.compose = true
    }

    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    dependencies {
        add("implementation", libs.findLibrary("androidxActivityCompose").get())
        add("implementation", libs.findLibrary("androidxComposeUi").get())
        add("implementation", libs.findLibrary("androidxComposeMaterial3").get())
        add("implementation", libs.findLibrary("androidxComposeUiToolingPreview").get())
        add("debugImplementation", libs.findLibrary("androidxComposeUiTooling").get())

        add("testImplementation", libs.findLibrary("junit").get())
        add("androidTestImplementation", libs.findLibrary("androidxJunit").get())
        add("androidTestImplementation", libs.findLibrary("androidxEspressoCore").get())
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(buildComposeMetricsParameters())
        }
    }
}

private fun Project.buildComposeMetricsParameters(): List<String> {
    val metricParameters = mutableListOf<String>()
    val enableMetricsProvider = providers.gradleProperty("enableComposeCompilerMetrics")
    if (enableMetricsProvider.orNull == "true") {
        val metricsFolder = File(layout.buildDirectory.asFile.get(), "compose-metrics")
        metricParameters.add("-P")
        metricParameters.add(
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${metricsFolder.absolutePath}"
        )
    }

    val enableReportsProvider = providers.gradleProperty("enableComposeCompilerReports")
    if (enableReportsProvider.orNull == "true") {
        val reportsFolder = File(layout.buildDirectory.asFile.get(), "compose-reports")
        metricParameters.add("-P")
        metricParameters.add(
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${reportsFolder.absolutePath}"
        )
    }
    return metricParameters
}