package com.havenpad

import com.havenpad.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KotlinSerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            
            // Only add dependencies if it's not a KMP project (handled in KMP plugin)
            // Or just add it anyway, it will be deduped or used correctly.
            // Actually, for KMP we usually want it in commonMain.
            // Let's just apply the plugin here. Dependencies are handled in modules or specific plugins.
        }
    }
}
