private object Versions {
    const val kotlin = "1.3.50"
    const val spek = "2.0.7"
}

object Dep {
    val plugins = Plugins
    val kotlin = Kotlin
    val android = Android

    object Plugins {
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
        const val ktlint = "org.jlleitschuh.gradle:ktlint-gradle:9.0.0"
        const val gradleVersions = "com.github.ben-manes:gradle-versions-plugin:0.25.0"
    }

    object Kotlin {
        private val v = Versions

        const val std = "org.jetbrains.kotlin:kotlin-stdlib:${v.kotlin}"
        const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${v.kotlin}"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:${v.kotlin}"
        const val kotlinAssertions = "io.kotlintest:kotlintest-assertions:3.4.2"
        const val junit = "junit:junit:4.12"
        const val rxJava = "io.reactivex.rxjava2:rxjava:2.2.13"
        const val spekJvm = "org.spekframework.spek2:spek-dsl-jvm:${v.spek}"
        const val spekRuntime = "org.spekframework.spek2:spek-runner-junit5:${v.spek}"
    }

    object Android {
        private val v = Versions

        const val appCompat = "androidx.appcompat:appcompat:1.1.0"
        const val coreKtx = "androidx.core:core-ktx:1.2.0-alpha04"
        const val lifecycleExt = "androidx.lifecycle:lifecycle-extensions:2.2.0-alpha05"
        const val rxAndroid = "io.reactivex.rxjava2:rxandroid:2.1.1"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.0-beta2"
        const val picasso = "com.squareup.picasso:picasso:2.71828"
        const val loadingButton = "br.com.simplepass:loading-button-android:2.1.5"
    }
}