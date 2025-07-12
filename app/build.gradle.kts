plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Google Services plugin for Firebase
}

android {
    namespace = "com.gtemedia.tinago"
    compileSdk = 35 // Ensure this is consistent with targetSdk

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
    // AndroidX and Material Design UI components
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material) // Material Design components (for TextInputLayout, Buttons, etc.)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview) // For CardView in item_citizen_vehicle and item_stolen_vehicle

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase Platform (Bill of Materials) - recommended for managing Firebase versions
    // Use only one Firebase BOM. The 'v3300' seems to be a specific version alias.
    // It's best to use the most up-to-date stable BOM.
    implementation(platform(libs.firebase.bom)) // Keep this, assuming 'libs.firebase.bom' points to the latest stable BOM

    // Firebase SDKs
    implementation(libs.firebase.analytics) // For Firebase Analytics (optional, but good for insights)
    implementation(libs.firebase.auth.ktx) // Firebase Authentication with Kotlin extensions
    implementation(libs.firebase.firestore.ktx) // Firestore Database with Kotlin extensions
    implementation(libs.firebase.messaging.ktx) // Firebase Cloud Messaging with Kotlin extensions
    implementation(libs.firebase.storage.ktx) // Firebase Storage with Kotlin extensions (for images/files)

    // ZXing Android Embedded (for QR Code Scanning)
    // Only one implementation needed. 'libs.zxing.android.embedded' should be sufficient.
    implementation(libs.zxing.android.embedded)
    // com.google.zxing:core is a transitive dependency of zxing-android-embedded,
    // so it's usually not necessary to declare it explicitly unless you need a specific version.
    // If you encounter issues, you might need to add it with a specific version.
    // implementation(libs.core)

    // Image Loading Library (Glide)
    implementation(libs.glide)
    annotationProcessor(libs.compiler) // Annotation processor for Glide

    // CircleImageView (for circular image views, if used)
    implementation(libs.circleimageview)

    // Image Slideshow (if used, not directly seen in provided layouts but kept if planned)
    implementation(libs.imageslideshow)

    // Removed Jetpack Compose dependencies as your project uses View-based (XML) UI
    // If you plan to use Compose, these would be needed, but they are unnecessary for XML-based UI.
    // implementation(platform(libs.androidx.compose.bom))
    // implementation(libs.androidx.ui)
    // implementation(libs.androidx.ui.graphics)
    // implementation(libs.androidx.ui.tooling.preview)
    // implementation(libs.androidx.material3)
    // debugImplementation(libs.androidx.ui.tooling)
    // debugImplementation(libs.androidx.ui.test.manifest)
    // implementation(libs.androidx.lifecycle.runtime.ktx)
    // implementation(libs.androidx.activity.compose)

    // Removed Retrofit, Gson, Converter-Gson as they are not explicitly used by Firebase
    // and your current Kotlin files don't show external API calls.
    // If you later integrate external APIs (e.g., OpenALPR), you'll need to re-add these.
    // implementation(libs.retrofit)
    // implementation(libs.converter.gson)
    // implementation(libs.gson)
}
