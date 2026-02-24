plugins {
    `kotlin-dsl`
    alias(libs.plugins.buildconfig)
}

fun Provider<PluginDependency>.asDep() =
    map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

dependencies {
    implementation(libs.plugins.spotless.asDep())
    implementation(libs.plugins.errorprone.asDep())
    implementation(libs.plugins.animalsniffer.asDep())
    implementation(libs.plugins.vanniktech.publish.asDep())
}

// Expose tool versions from this catalog to precompiled script plugins
// (workaround for gradle/gradle#15383)
buildConfig {
    packageName("")
    buildConfigField("CHECKSTYLE", libs.versions.checkstyle)
    buildConfigField("ERROR_PRONE", libs.versions.error.prone.core)
    buildConfigField("JACOCO", libs.versions.jacoco)
    buildConfigField("PALANTIR_FORMAT", libs.versions.palantir.format)
    buildConfigField("GUMMY_BEARS", libs.versions.gummy.bears)
}
