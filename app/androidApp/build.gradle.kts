plugins {
    alias(libs.plugins.rotdAndroidApplication)
    alias(libs.plugins.rotdAndroidApplicationFirebase)
    alias(libs.plugins.rotdAndroidPresentationExtras)
    alias(libs.plugins.rotdAndroidKoin)
}

dependencies {
    implementation(project(":app:shared"))
    // Used in KejaSafeApplication to clear Firebase Auth on cold start.
    // (core holds auth as implementation, so it is not leaked to this module.)
    implementation(libs.firebaseAuth)
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "io.grpc:grpc-api:1.62.2",
            "io.grpc:grpc-context:1.62.2",
            "io.grpc:grpc-core:1.62.2",
            "io.grpc:grpc-okhttp:1.62.2",
            "io.grpc:grpc-protobuf-lite:1.62.2",
            "io.grpc:grpc-stub:1.62.2",
            "io.grpc:grpc-android:1.62.2",
            "io.grpc:grpc-util:1.62.2",
        )
    }
}

android {
    namespace = "com.rotd.kejasafe"

    defaultConfig {
        applicationId = "com.rotd.kejasafe"
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}