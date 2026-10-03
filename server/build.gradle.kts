plugins {
    alias(libs.plugins.rotdServerLibrary)
    alias(libs.plugins.ktor)
}

group = "com.rotd.havenpad"
version = "1.0.0"

application {
    mainClass = "com.rotd.havenpad.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.ktorServerWebsockets)
    implementation(libs.ktorClientOkhttp)
    implementation(libs.googleAuthOauth)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
}
