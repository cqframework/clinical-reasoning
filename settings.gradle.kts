pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "cqf-fhir"

include("cqf-fhir-utility")
include("cqf-fhir-test")
include("cqf-fhir-cql")
include("cqf-fhir-cr")
include("cqf-fhir-bom")
include("cqf-fhir-cr-hapi")
include("cqf-fhir-cr-spring")
include("cqf-fhir-cr-cli")
include("cqf-fhir-benchmark")
include("docs")
