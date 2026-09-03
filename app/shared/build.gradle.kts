plugins {
    alias(libs.plugins.rotdKotlinMultiplatformLibrary)
}

kotlin {

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    androidLibrary {
        namespace = "com.rotd.kejasafe.app.shared"
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.composeUiTooling)
        }
        commonMain.dependencies {
            api(project(":core"))
            api(project(":feature:rent"))
            api(project(":feature:profile"))
            api(project(":feature:listing"))
            api(project(":feature:chat"))
            api(project(":feature:call"))
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.composeUiTooling)
}