plugins { id("com.android.library") }

// The original SDK is pinned in UPSTREAM.json. Only Android QR, device and screen helpers
// are built here. The application's CMake build owns the one native SDK instance.
android {
    namespace = "com.google.cardboard.sdk"
    // Preserve pinned upstream source; baseline only reviewed permission-guard findings.
    lint { baseline = file("lint-baseline.xml") }
    compileSdk = 36
    defaultConfig { minSdk = 29; consumerProguardFiles("vendor/cardboard/sdk/proguard-rules.pro") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    sourceSets["main"].apply {
        java.srcDirs("vendor/cardboard/sdk/qrcode/android/java", "vendor/cardboard/sdk/device_params/android/java",
            "vendor/cardboard/sdk/screen_params/android/java", "vendor/cardboard/sdk/java_utils/android/java")
        res.srcDir("vendor/cardboard/sdk/qrcode/android/res")
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.gms:play-services-vision:20.1.3")
    implementation("com.google.android.material:material:1.12.0")
    // Regenerated from the pinned SDK's unchanged proto with this exact supported runtime.
    api("com.google.protobuf:protobuf-javalite:4.36.2")
    implementation("org.jspecify:jspecify:1.0.0")
}
