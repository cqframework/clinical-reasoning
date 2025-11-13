plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.gradle.spotless:com.diffplug.gradle.spotless.gradle.plugin:8.0.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
}
