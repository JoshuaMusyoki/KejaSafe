import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    `kotlin-dsl`
}

group = "com.rotd.havenpad.buildlogic"

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("comAndroidKotlinMultiplatformLibraryGradlePlugin").get())
    compileOnly(libs.findLibrary("orgJetbrainsKotlinMultiplatformGradlePlugin").get())
    compileOnly(libs.findLibrary("orgJetbrainsComposeGradlePlugin").get())
    compileOnly(libs.findLibrary("orgJetbrainsKotlinPluginComposeGradlePlugin").get())
}
