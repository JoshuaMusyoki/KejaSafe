import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.kejasafe.convention"

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}


dependencies {
    implementation(libs.findLibrary("androidGradlePlugin").get())
    implementation(libs.findLibrary("comAndroidKotlinMultiplatformLibraryGradlePlugin").get())
    implementation(libs.findLibrary("orgJetbrainsKotlinMultiplatformGradlePlugin").get())
    implementation(libs.findLibrary("orgJetbrainsComposeGradlePlugin").get())
    implementation(libs.findLibrary("orgJetbrainsKotlinPluginComposeGradlePlugin").get())
    implementation(libs.findLibrary("orgJetbrainsKotlinPluginSerializationGradlePlugin").get())
    implementation(libs.findLibrary("firebaseCrashlyticsGradle").get())
    implementation(libs.findLibrary("firebasePerfGradle").get())
    implementation(libs.findLibrary("firebaseAppdistributionGradle").get())
    implementation(libs.findLibrary("googleServicesGradle").get())
    implementation(libs.findLibrary("koinGradlePlugin").get())
    implementation(libs.findLibrary("kspGradlePlugin").get())
    testImplementation(libs.findLibrary("junit").get())
    compileOnly(libs.findLibrary("kotlinGradlePlugin").get())
}

gradlePlugin {
    plugins {
        register("rotdAndroidLibrary") {
            id = "rotd.android.library"
            implementationClass = "com.kejasafe.AndroidLibraryConventionPlugin"
        }

        register("rotdAndroidApplication") {
            id = "rotd.android.application"
            implementationClass = "com.kejasafe.AndroidApplicationConventionPlugin"
        }
        register("rotdAndroidRoom") {
            id = "rotd.android.room"
            implementationClass = "com.kejasafe.AndroidRoomConventionPlugin"
        }
        register("rotdAndroidKoin") {
            id = "rotd.android.koin"
            implementationClass = "com.kejasafe.AndroidKoinConventionPlugin"
        }
        register("rotdAndroidLibraryCompose") {
            id = "rotd.android.library.compose"
            implementationClass = "com.kejasafe.AndroidLibraryComposeConventionPlugin"
        }
        register("rotdAndroidApplicationFirebase") {
            id = "rotd.android.application.firebase"
            implementationClass = "com.kejasafe.AndroidApplicationFirebaseConventionPlugin"
        }
        register("rotdAndroidLibraryFirebase") {
            id = "rotd.android.library.firebase"
            implementationClass = "com.kejasafe.AndroidLibraryFirebaseConventionPlugin"
        }
        register("rotdAndroidNetwork") {
            id = "rotd.android.network"
            implementationClass = "com.kejasafe.AndroidNetworkConventionPlugin"
        }
        register("rotdAndroidFeature") {
            id = "rotd.android.feature"
            implementationClass = "com.kejasafe.AndroidFeatureConventionPlugin"
        }
        register("rotdAndroidCompose") {
            id = "rotd.android.compose"
            implementationClass = "com.kejasafe.AndroidComposeConventionPlugin"
        }
        register("rotdAndroidFirebase") {
            id = "rotd.android.firebase"
            implementationClass = "com.kejasafe.AndroidFirebaseConventionPlugin"
        }
        register("rotdAndroidPresentationExtras") {
            id = "rotd.android.presentation.extras"
            implementationClass = "com.kejasafe.AndroidPresentationExtrasConventionPlugin"
        }
        register("rotdKotlinMultiplatformLibrary") {
            id = "rotd.kotlin.multiplatform.library"
            implementationClass = "com.kejasafe.KotlinMultiplatformLibraryConventionPlugin"
        }
        register("rotdKotlinMultiplatformRoom") {
            id = "rotd.kotlin.multiplatform.room"
            implementationClass = "com.kejasafe.KotlinMultiplatformRoomConventionPlugin"
        }
        register("rotdKotlinSerialization") {
            id = "rotd.kotlin.serialization"
            implementationClass = "com.kejasafe.KotlinSerializationConventionPlugin"
        }
        register("rotdServerLibrary") {
            id = "rotd.server.library"
            implementationClass = "com.kejasafe.AndroidLibraryServerConventionPlugin"
        }
    }
}
repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}
