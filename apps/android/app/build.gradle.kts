plugins { id("com.android.application") }

android {
    namespace = "com.narvyn.suraksha"
    compileSdk = 36
    ndkVersion = "27.1.12297006"
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "com.narvyn.suraksha"
        minSdk = 29
        targetSdk = 36
        versionCode = 41
        versionName = "0.21.0-dev"
        val apiOrigin = System.getenv("SURAKSHA_API_ORIGIN") ?: ""
        require(apiOrigin.isEmpty() || (apiOrigin.startsWith("https://") && apiOrigin.matches(Regex("https://[A-Za-z0-9.-]+(:[0-9]+)?")))) { "SURAKSHA_API_ORIGIN must be an HTTPS origin" }
        buildConfigField("String", "API_ORIGIN", "\"$apiOrigin\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        externalNativeBuild { cmake { arguments += "-DANDROID_STL=c++_static" } }
    }
    signingConfigs {
        create("distribution") {
            val keyPath = System.getenv("SURAKSHA_ANDROID_KEYSTORE")
            if (keyPath != null) {
                storeFile = file(keyPath)
                storePassword = System.getenv("SURAKSHA_ANDROID_STORE_PASSWORD")
                keyAlias = System.getenv("SURAKSHA_ANDROID_KEY_ALIAS") ?: "surakshaar-release"
                keyPassword = System.getenv("SURAKSHA_ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            if (System.getenv("SURAKSHA_ANDROID_SIGN_DEBUG") == "true" && System.getenv("SURAKSHA_ANDROID_KEYSTORE") != null) signingConfig = signingConfigs.getByName("distribution")
        }
        release {
            isMinifyEnabled = false
            if (System.getenv("SURAKSHA_ANDROID_KEYSTORE") != null) signingConfig = signingConfigs.getByName("distribution")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    // Keep emulator window transitions from obscuring injected UI test touches.
    testOptions { animationsDisabled = true }
    sourceSets["main"].assets.srcDir("../../../content")
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
}
dependencies {
    implementation(project(":cardboard-sdk"))
    implementation("androidx.core:core:1.17.0")
    implementation("com.google.ar:core:1.56.0")
    implementation("com.google.mediapipe:tasks-vision:1.1.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
