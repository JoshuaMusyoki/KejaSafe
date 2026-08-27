plugins {
    alias(libs.plugins.rotdServerLibrary)
    alias(libs.plugins.ktor)
}

group = "com.rotd.kejasafe"
version = "1.0.0"

application {
    mainClass = "com.rotd.kejasafe.ApplicationKt"
}

dependencies {
    api(project(":core"))
}
