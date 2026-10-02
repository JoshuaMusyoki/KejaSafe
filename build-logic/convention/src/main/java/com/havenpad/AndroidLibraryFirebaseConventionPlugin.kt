package com.havenpad

import com.havenpad.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                val bom = libs.findLibrary("firebaseBom").get()
                add("implementation", platform(bom))
                "implementation"(libs.findLibrary("firebaseAnalytics").get())
                "implementation"(libs.findLibrary("firebasePerformance").get())
                "implementation"(libs.findLibrary("firebaseMessaging").get())
                "implementation"(libs.findLibrary("firebaseDatabase").get())
                "implementation"(libs.findLibrary("firebaseFirestore").get())
                "implementation"(libs.findLibrary("firebaseStorage").get())
                "implementation"(libs.findLibrary("firebaseAuth").get())
                "implementation"(libs.findLibrary("firebaseDataconnect").get())
            }
        }
    }
}

