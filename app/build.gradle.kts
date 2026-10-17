plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.keon.musicstream"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.keon.musicstream"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // Clé injectée au build via la variable d'environnement YT_API_KEY (secret GitHub).
        // Jamais écrite dans le code source.
        val ytKey = System.getenv("YT_API_KEY") ?: ""
        buildConfigField("String", "YT_API_KEY", "\"$ytKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Lecteur officiel YouTube IFrame Player encapsulé dans une WebView
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")
}
