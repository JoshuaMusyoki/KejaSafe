package com.havenpad

import com.havenpad.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            pluginManager.apply("com.google.devtools.ksp")

            dependencies {
                add("implementation", libs.findLibrary("androidxRoomRuntime").get())
                add("implementation", libs.findLibrary("androidxRoomKtx").get())
                add("ksp", libs.findLibrary("androidxRoomCompiler").get())
            }
        }
    }
}

