plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "pl.smoqbonus.clicker"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.smoqbonus.clicker"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
