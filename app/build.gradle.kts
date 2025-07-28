plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.lpavs.caliinda"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lpavs.caliinda"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val backendUrl: String = findProperty("BACKEND_BASE_URL") as? String ?: ""
        val webClientId: String = findProperty("BACKEND_WEB_CLIENT_ID") as? String ?: ""

        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendUrl\"")
        buildConfigField("String", "BACKEND_WEB_CLIENT_ID", "\"$webClientId\"")
    }
    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.googleid)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)
    implementation(libs.androidx.material)
    implementation(libs.material3)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.calendar)
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
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

// Turbine for Flow testing
    testImplementation(libs.turbine) // Используй актуальную версию

// Truth for assertions (optional but recommended)
    testImplementation(libs.truth)

// Tasks API for mocking Google Play Services Tasks
    testImplementation(libs.play.services.tasks) // Используй версию, совместимую с твоей основной

}