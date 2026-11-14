plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") // AppFunctions schema + service, and the Room DAO
}

android {
    namespace = "dev.omerdurmaz.notes"
    compileSdk { version = release(37) } // the AppFunctions AAR declares minCompileSdk 37

    defaultConfig {
        applicationId = "dev.omerdurmaz.notes"
        minSdk = 26 // the library supports 24+; functions only run on API 36+
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    testOptions { unitTests.isIncludeAndroidResources = true } // Robolectric
}

dependencies {
    implementation("androidx.appfunctions:appfunctions:1.0.0-rc01")
    ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-rc01")
    // The generated service uses coroutines; the AAR only brings them in at runtime.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    testImplementation("androidx.appfunctions:appfunctions-testing:1.0.0-rc01")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")

    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
