plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.lpavs.caliinda"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lpavs.caliinda"
        minSdk = 32
        targetSdk = 35
        versionCode = 8
        versionName = "Breakfast-1.21"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.junit)
    implementation(libs.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)
    implementation(libs.androidx.material)
    implementation(libs.material3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Core testing libraries
    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing) // Для InstantTaskExecutorRule (хотя тут не критично)
    testImplementation(libs.kotlinx.coroutines.test) // Используй актуальную версию
    testImplementation(kotlin("test"))

// Mockito for mocking
    testImplementation(libs.kotlin.mockito.kotlin) // Используй актуальную версию
    testImplementation(libs.mockito.inline) // Для мокания final классов/методов, если нужно
    testImplementation(libs.robolectric)
// Turbine for Flow testing
    testImplementation(libs.turbine) // Используй актуальную версию

// Truth for assertions (optional but recommended)
    testImplementation(libs.truth)

    testImplementation(libs.hilt.android.testing)
}