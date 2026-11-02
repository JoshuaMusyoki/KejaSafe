package com.havenpad

import com.havenpad.convention.libs
import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val googleServicesJson = sequenceOf(
                file("google-services.json"),
                file("src/google-services.json"),
                file("src/debug/google-services.json"),
                file("src/release/google-services.json"),
                // Common misplacement: app/google-services.json (one level above androidApp)
                rootProject.file("app/google-services.json"),
                file("../google-services.json")
            ).firstOrNull { it.exists() }

            if (googleServicesJson == null) {
                logger.warn(
                    "google-services.json not found for '{}'. " +
                        "Skipping Google Services / Crashlytics / Perf plugins so the app can still build. " +
                        "Add the file from Firebase Console when you enable Firebase.",
                    path
                )
            } else {
                with(pluginManager) {
                    apply("com.google.gms.google-services")
                    apply("com.google.firebase.firebase-perf")
                    apply("com.google.firebase.crashlytics")
                }

                extensions.configure<ApplicationExtension> {
                    buildTypes.configureEach {
                        configure<CrashlyticsExtension> {
                            mappingFileUploadEnabled = false
                        }
                    }
                }
            }

            // App Distribution does not require google-services.json at configure time.
            pluginManager.apply("com.google.firebase.appdistribution")

            dependencies {
                val bom = libs.findLibrary("firebaseBom").get()
                add("implementation", platform(bom))
                add("implementation", libs.findLibrary("firebaseAnalytics").get())
                add("implementation", libs.findLibrary("firebasePerformance").get())
                add("implementation", libs.findLibrary("firebaseCrashlytics").get())
                add("implementation", libs.findLibrary("firebaseCommon").get())
                add("implementation", libs.findLibrary("firebaseRemoteConfig").get())
                add("implementation", libs.findLibrary("firebaseConfig").get())
            }
        }
    }
}
