package com.kejasafe

import com.kejasafe.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                val bom = libs.findLibrary("firebaseBom").get()
                add("implementation", platform(bom))
                add("implementation", libs.findLibrary("firebaseAuth").get())
                add("implementation", libs.findLibrary("firebaseDataconnect").get())
                add("implementation", libs.findLibrary("firebaseFirestore").get())
                add("implementation", libs.findLibrary("firebaseMessaging").get())
                add("implementation", libs.findLibrary("firebaseStorage").get())
                add("implementation", libs.findLibrary("firebaseFunctions").get())
                add("implementation", libs.findLibrary("firebaseAnalytics").get())
                add("implementation", libs.findLibrary("firebasePerformance").get())
                add("implementation", libs.findLibrary("firebaseCrashlytics").get())
                // google-api-client (libs.googleAuth) is JVM-only — do not add on Android;
                // it upgrades grpc-api past firebase-dataconnect's grpc-core and crashes login.
            }
        }
    }
}
