plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
    id("cqf.jacoco-conventions")
    id("cqf.animal-sniffer-conventions")
    id("cqf.publishing-conventions")
}

dependencies {
    implementation(libs.hapi.fhir.caching.caffeine)
    testImplementation(libs.jsonassert)
}
