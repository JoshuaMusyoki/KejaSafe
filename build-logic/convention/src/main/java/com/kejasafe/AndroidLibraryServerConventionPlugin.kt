package com.kejasafe

import com.kejasafe.convention.configureKotlinJvm
import com.kejasafe.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryServerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
            }

            configureKotlinJvm()

            dependencies {
                add("implementation", libs.findLibrary("logback").get())
                add("implementation", libs.findLibrary("ktorServerCore").get())
                add("implementation", libs.findLibrary("ktorServerNetty").get())
                add("implementation", libs.findLibrary("ktorServerAuth").get())
                add("implementation", libs.findLibrary("ktorServerAuthJwt").get())
                add("implementation", libs.findLibrary("ktorServerContentNegotiation").get())
                add("implementation", libs.findLibrary("ktorSerializationJson").get())

                add("implementation", libs.findLibrary("exposedCore").get())
                add("implementation", libs.findLibrary("exposedDao").get())
                add("implementation", libs.findLibrary("exposedJdbc").get())
                add("implementation", libs.findLibrary("h2").get())
                add("implementation", libs.findLibrary("googleAuth").get())
                add("implementation", libs.findLibrary("bcrypt").get())

                add("testImplementation", libs.findLibrary("ktorServerTestHost").get())
                add("testImplementation", libs.findLibrary("kotlinTestJunit").get())
            }
        }
    }
}
