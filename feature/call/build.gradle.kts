plugins {
    alias(libs.plugins.rotdKotlinMultiplatformLibrary)
    alias(libs.plugins.rotdKotlinSerialization)
}

kotlin {
    android {
        namespace = "com.rotd.havenpad.feature.call"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.androidxLifecycleRuntimeCompose)
            implementation(libs.androidxLifecycleViewmodelCompose)
        }
        androidMain.dependencies {
            implementation(libs.webRtc)
            implementation(libs.accompanistPermissions)
        }
    }
}
