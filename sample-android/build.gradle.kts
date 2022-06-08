plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.androidJunit5)
}

android {
    compileSdk = 32
    defaultConfig {
        applicationId = "kelm.sample"
        minSdk = 23
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
    }
}

dependencies {
    implementation(project(":kelm-core"))
    implementation(project(":kelm-android"))

    implementation(libs.android.coroutinesAndroid)
    implementation(libs.android.appCompat)
    implementation(libs.android.activity) {
        version {
            strictly("1.5.0-rc01")
        }
    }
    implementation(libs.android.activityCompose)
    implementation(libs.android.coreKtx)
    implementation(libs.android.lifecycleKtx)
    implementation(libs.android.lifecycleViewModelKtx)
    implementation(libs.android.compose.ui)
    implementation(libs.android.compose.material)
    implementation(libs.android.compose.tooling)
    implementation(libs.android.coil.compose)

    testImplementation(libs.kotlin.junit5.jupiter)
    testImplementation(libs.kotlin.kotlinTest)
    testImplementation(libs.kotlin.kotlinAssertions)
}
