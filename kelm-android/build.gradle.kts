plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
    signing
}

android {
    namespace = "kelm.android"
    compileSdk = 32

    defaultConfig {
        minSdk = 21
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":kelm-core"))

    implementation(libs.kotlin.coroutines)
    implementation(libs.android.lifecycleViewModelKtx)
    implementation(libs.android.lifecycleLiveDataKtx)
}

publishing {
    repositories {
        maven {
            val releasesRepoUrl =
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            )
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = project.property("kelm.publish.groupId") as String
            artifactId = "kelm-android"
            version = project.property("kelm.publish.version") as String

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Kelm")
                description.set(
                    "Kelm simplifies management of complex app states and asynchronous tasks by enforcing a pattern\n" +
                        "based on the Elm Architecture."
                )
                url.set("http://github.com/AllanHasegawa/kelm")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("AllanHasegawa")
                        name.set("Allan Hasegawa")
                        email.set("hasegawa.aran@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:AllanHasegawa/kelm.git")
                    developerConnection.set("scm:git:git@github.com:AllanHasegawa/kelm.git")
                    url.set("http://github.com/AllanHasegawa/kelm")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
