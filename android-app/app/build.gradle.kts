plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nothing.widgets"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nothing.widgets"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        manifestPlaceholders["spotifyRedirectScheme"] = "nothingwidgets"
        manifestPlaceholders["spotifyRedirectHost"] = "spotify-callback"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // WorkManager for background widget sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // HTTP networking & JSON for Spotify Web API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Image loading for album art in widgets & companion
    implementation("io.coil-kt:coil:2.6.0")
    
    // Security & Encrypted storage for OAuth tokens
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.browser:androidx.browser:1.7.0")
}
