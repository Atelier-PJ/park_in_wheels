plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.parkonwheels.demo"
    compileSdk = 35
    useLibrary("android.car", false)
    defaultConfig {
        applicationId = "com.parkonwheels.demo"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-demo"
    }
    flavorDimensions += "vehicle"
    productFlavors {
        create("demo") {
            dimension = "vehicle"
            manifestPlaceholders["appLabel"] = "Park on Wheels · DEMO"
        }
        create("pleos") {
            dimension = "vehicle"
            applicationId = "com.parkonwheels.pleos"
            minSdk = 29
            manifestPlaceholders["appLabel"] = "Park on Wheels · SDK TEST"
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core"))
    "pleosImplementation"("ai.pleos.playground:Vehicle:2.3.2")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
}
