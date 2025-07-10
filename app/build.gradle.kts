plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.gtemedia.tinago"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gtemedia.tinago"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    implementation (libs.androidx.cardview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Add the Firebase BoM (Bill of Materials) - recommended way to manage Firebase versions
    implementation(platform(libs.firebase.bom)) // Check for latest version
    // Add the Firebase SDK for Google Analytics (recommended)
    implementation(libs.firebase.analytics)
    // Add the Firebase SDK for Authentication (essential for login)
    implementation(libs.firebase.auth.ktx)
    // Add the Firebase SDK for Fire_store (for database interactions)
    implementation(libs.firebase.firestore.ktx)
    // Add the Firebase SDK for Cloud Messaging (for push notifications)
    implementation(libs.firebase.messaging.ktx)
    // Add the Firebase SDK for Storage (if you need to store images/files)
    implementation(libs.firebase.storage.ktx)

    implementation (libs.circleimageview)

    implementation(platform(libs.firebase.bom.v3300)) // Use the latest BOM version
    implementation(libs.google.firebase.messaging.ktx) // For Kotlin extensions

    // ZXing Android Embedded (for QR Code Scanning)
    implementation(libs.zxing.android.embedded)
    implementation(libs.core) // Make sure to use a compatible core version

    implementation (libs.zxing.android.embedded)
    // Add other necessary libraries for networking (Retrofit) and JSON serialization
    implementation(libs.retrofit)
    implementation(libs.converter.gson) // Or converter-kotlinx-serialization
    implementation(libs.gson) // Or kotlinx-serialization for JSON

    // For modern Android UI development (Jetpack Compose)
// If you chose Empty Activity, it might already have some compose dependencies
// If not, add them:
    implementation(platform(libs.androidx.compose.bom)) // Check latest compose BOM
    implementation(libs.androidx.ui)
implementation(libs.androidx.ui.graphics)
implementation(libs.androidx.ui.tooling.preview)
implementation(libs.androidx.material3)
debugImplementation(libs.androidx.ui.tooling)
debugImplementation(libs.androidx.ui.test.manifest)
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.activity.compose)
    implementation (libs.imageslideshow)
    implementation (libs.glide)
    annotationProcessor (libs.compiler)

}
