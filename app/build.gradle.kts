plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "ye.alkamel.billing"
    compileSdk = 35
    defaultConfig {
        applicationId = "ye.alkamel.billing"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
