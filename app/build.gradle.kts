import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Load local.properties for API keys
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) load(localPropsFile.inputStream())
}

android {
    namespace = "com.sportspulse.india"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sportspulse.india"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // ─── API Keys as BuildConfig fields ───────────────────────────────
        buildConfigField(
            "String", "GOOGLE_MAPS_API_KEY",
            "\"${localProperties.getProperty("GOOGLE_MAPS_API_KEY", "YOUR_GOOGLE_MAPS_API_KEY")}\""
        )
        buildConfigField(
            "String", "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY")}\""
        )
        buildConfigField(
            "String", "SPORTRADAR_API_KEY",
            "\"${localProperties.getProperty("SPORTRADAR_API_KEY", "YOUR_SPORTRADAR_API_KEY")}\""
        )
        buildConfigField(
            "String", "CRICAPI_KEY",
            "\"${localProperties.getProperty("CRICAPI_KEY", "YOUR_CRICAPI_KEY")}\""
        )
        buildConfigField(
            "String", "FOOTBALL_DATA_API_KEY",
            "\"${localProperties.getProperty("FOOTBALL_DATA_API_KEY", "YOUR_FOOTBALL_DATA_API_KEY")}\""
        )
        buildConfigField(
            "String", "PLACES_API_KEY",
            "\"${localProperties.getProperty("PLACES_API_KEY", "YOUR_PLACES_API_KEY")}\""
        )
        buildConfigField(
            "String", "BROADCAST_CONFIG_GIST_URL",
            "\"${localProperties.getProperty("BROADCAST_CONFIG_GIST_URL", "https://gist.githubusercontent.com/your-user/your-gist-id/raw/broadcast_schedule.json")}\""
        )

        // ─── Manifest placeholders ─────────────────────────────────────────
        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("GOOGLE_MAPS_API_KEY", "YOUR_GOOGLE_MAPS_API_KEY")
    }

    buildTypes {
        debug {
            isDebuggable = true
            versionNameSuffix = "-debug"
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }

    // Allow Rome RSS library (uses older Java XML APIs)
    lint {
        abortOnError = false
        disable += "MissingTranslation"
    }
}

dependencies {
    // ─── Core Android ────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // ─── Compose BOM ─────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ─── Navigation ──────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ─── Hilt DI ─────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // ─── Room ────────────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ─── DataStore ───────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ─── Networking ──────────────────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // ─── Coil ────────────────────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ─── Coroutines ──────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ─── Location + Maps ─────────────────────────────────────────────────
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)

    // ─── WorkManager + Hilt ──────────────────────────────────────────────
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // ─── Accompanist ─────────────────────────────────────────────────────
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.accompanist.placeholder.material3)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // ─── Splash Screen ───────────────────────────────────────────────────
    implementation(libs.androidx.core.splashscreen)

    // ─── Firebase AI Logic ───────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)

    // ─── RSS (Rome) ──────────────────────────────────────────────────────
    implementation(libs.rome)

    // ─── Kotlin Serialization ────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ─── Timber Logging ──────────────────────────────────────────────────
    implementation(libs.timber)

    // ─── Security ────────────────────────────────────────────────────────
    implementation(libs.androidx.security.crypto)

    // ─── Biometric (App Lock) ────────────────────────────────────────
    implementation(libs.androidx.biometric)

    // ─── Lifecycle Process (Foreground detection) ─────────────────────
    implementation(libs.androidx.lifecycle.process)

    // ─── Testing ─────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
