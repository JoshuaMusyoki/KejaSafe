plugins {
    alias(libs.plugins.rotdKotlinMultiplatformLibrary)
    alias(libs.plugins.rotdKotlinSerialization)
    alias(libs.plugins.rotdKotlinMultiplatformRoom)
}

kotlin {
    android {
        namespace = "com.rotd.kejasafe.core"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebaseBom))
            implementation(libs.firebaseAuth)
            implementation(libs.firebaseDataconnect)
            implementation(libs.kotlinxCoroutinesPlayServices)
            implementation(libs.androidxBiometric)
            implementation(libs.androidxSecurityCrypto)
            implementation(libs.androidxFragmentKtx)
            implementation(libs.grpcAndroid)
            implementation(libs.grpcOkhttp)
            implementation(libs.grpcProtobufLite)
            implementation(libs.grpcStub)
        }
        commonTest.dependencies {
            implementation(libs.kotlinxCoroutinesTest)
        }
    }
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
