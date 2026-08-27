package com.kejasafe

import com.kejasafe.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidKoinConventionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            /* 
               Koin Gradle plugin is not strictly required unless using Koin Compiler Plugin.
               Applying it here often causes resolution issues if the plugin artifact 
               is not correctly configured in build-logic classpath.
            */
            // with(pluginManager){
            //    apply("io.insert-koin")
            // }
            dependencies {
                add("implementation", libs.findLibrary("koinAndroid").get())
                add("implementation", libs.findLibrary("koinCoreViewModel").get())
                add("implementation", libs.findLibrary("koinComposeViewmodel").get())
                add("implementation", libs.findLibrary("koinAndroidxCompose").get())
                add("testImplementation", libs.findLibrary("koinTest").get())
                add("androidTestImplementation", libs.findLibrary("koinAndroidTest").get())
            }
        }
    }
}

