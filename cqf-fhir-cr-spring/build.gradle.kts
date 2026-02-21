plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
    id("cqf.jacoco-conventions")
    id("cqf.publishing-conventions")
}

// No animal-sniffer for this module

dependencies {
    api(project(":cqf-fhir-cr"))
    implementation(libs.spring.test)
    implementation(libs.spring.context)
}
