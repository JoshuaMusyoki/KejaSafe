plugins {
    alias(libs.plugins.rotdKotlinMultiplatformLibrary)
    alias(libs.plugins.rotdKotlinSerialization)
}

kotlin {
    android {
        namespace = "com.rotd.kejasafe.feature.chat"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.androidxLifecycleRuntimeCompose)
            implementation(libs.androidxLifecycleViewmodelCompose)
        }
    }
}
