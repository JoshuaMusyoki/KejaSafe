plugins {
    alias(libs.plugins.rotdKotlinMultiplatformLibrary)
    alias(libs.plugins.rotdKotlinSerialization)
}

kotlin {
    android {
        namespace = "com.rotd.kejasafe.feature.listing"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            // Explicit: IDE/commonMain need this for collectAsStateWithLifecycle
            implementation(libs.androidxLifecycleRuntimeCompose)
            implementation(libs.androidxLifecycleViewmodelCompose)
        }
    }
}
