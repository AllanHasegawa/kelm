plugins {
    kotlin("jvm")
    id("maven-publish")
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

group = "com.github.AllanHasegawa.kelm"
