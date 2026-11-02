package com.havenpad

import com.havenpad.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("implementation", libs.findLibrary("kotlinxCoroutinesCore").get())
                add("implementation", libs.findLibrary("kotlinxCoroutinesAndroid").get())
                add("implementation", libs.findLibrary("androidxLifecycleRuntimeKtx").get())
                add("implementation", libs.findLibrary("androidxLifecycleViewmodelCompose").get())
                add("implementation", libs.findLibrary("androidxLifecycleRuntimeCompose").get())
                add("implementation", libs.findLibrary("androidxLifecycleProcess").get())
                
                add("implementation", libs.findLibrary("koinAndroid").get())
                add("implementation", libs.findLibrary("koinCompose").get())
                add("implementation", libs.findLibrary("koinComposeViewmodel").get())
                add("implementation", libs.findLibrary("koinWorkmanager").get())
            }
        }
    }
}
