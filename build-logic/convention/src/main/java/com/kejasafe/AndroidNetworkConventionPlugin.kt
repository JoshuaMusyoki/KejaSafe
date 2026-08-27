package com.kejasafe

import com.kejasafe.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidNetworkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("implementation", libs.findLibrary("retrofit").get())
                add("implementation", libs.findLibrary("converterMoshi").get())
                add("implementation", libs.findLibrary("moshiKotlin").get())
                add("implementation", libs.findLibrary("okhttp").get())
                add("implementation", libs.findLibrary("loggingInterceptor").get())
                
                add("implementation", libs.findLibrary("ktorClientCore").get())
                add("implementation", libs.findLibrary("ktorClientAndroid").get())
                add("implementation", libs.findLibrary("ktorClientContentNegotiation").get())
                add("implementation", libs.findLibrary("ktorSerializationKotlinxJson").get())
                add("implementation", libs.findLibrary("ktorClientLogging").get())
            }
        }
    }
}
