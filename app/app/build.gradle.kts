plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.innovativeinside.pitchside"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.innovativeinside.pitchside"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Base URL of the Pitchside REST gateway (see API_CONTRACTS.md). Empty means the app runs on the
        // built-in mock data source. Set it with -PpitchsideApiBaseUrl=https://... or in gradle.properties.
        val apiBase = (project.findProperty("pitchsideApiBaseUrl") as String?) ?: ""
        buildConfigField("String", "API_BASE_URL", "\"$apiBase\"")

        // apifootball.com v3 key. When set, the app reads football data straight from apifootball.com (see
        // data/remote/ApiFootballComApi.kt). Keep it out of the repo: put apifootballKey=... in ~/.gradle/gradle.properties
        // (or $GRADLE_USER_HOME/gradle.properties), or pass -PapifootballKey=... . Empty falls back to the gateway or mock data.
        val apifootballKey = ((project.findProperty("apifootballKey") as String?) ?: "").trim()
        buildConfigField("String", "APIFOOTBALL_KEY", "\"$apifootballKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                // Conscrypt has no native build for every host (e.g. linux-aarch64); the tests don't need TLS.
                it.systemProperty("robolectric.conscryptMode", "OFF")
                it.maxHeapSize = "2g"
            }
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "ObsoleteLintCustomCheck")
    }
}

dependencies {
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
    implementation(libs.splashscreen)
    // Coil: async image loading with memory/disk cache and downsampling for the 90min news photos.
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(bom)
    testImplementation(libs.compose.ui.test.junit4)

    androidTestImplementation(bom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
