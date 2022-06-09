import java.io.File
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.kotlin.coroutines)

    testImplementation(libs.kotlin.junit5.jupiter)
    testImplementation(libs.kotlin.kotlinTest)
    testImplementation(libs.kotlin.kotlinAssertions)
    testImplementation(libs.kotlin.kotlinReflect)
    testImplementation(libs.kotlin.coroutinesTest)
    testImplementation(libs.kotlin.turbine)
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    systemProperty("junit.jupiter.conditions.deactivate", "*")
    systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.allanhasegawa.kelm"
            artifactId = "kelm-core"
            version = "2.0.0-alpha01"

            from(components["java"])

            pom {
                name.set("Kelm")
                description.set("Kelm simplifies management of complex app states and asynchronous tasks by enforcing a pattern\n" +
                    "based on the Elm Architecture.")
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
