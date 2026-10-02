package com.havenpad

import com.havenpad.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidPresentationExtrasConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                // Do NOT add Apache POI / documents on Android — poi-ooxml pulls Log4j
                // which requires MethodHandles (API 26+) and breaks minSdk 24 dexing.
                add("implementation", libs.findBundle("presentationExtras").get())

                // Common UI libs often used together with extras
                add("implementation", libs.findLibrary("androidxAppcompat").get())
                add("implementation", libs.findLibrary("androidMaterial").get())
            }
        }
    }
}
