plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
    id("cqf.jacoco-conventions")
    id("cqf.animal-sniffer-conventions")
    id("cqf.publishing-conventions")
}

dependencies {
    api(project(":cqf-fhir-utility"))
    api(libs.cql.cqf.fhir) {
        exclude(group = "junit", module = "junit")
    }
    api(libs.cql.cqf.fhir.npm) {
        exclude(group = "junit", module = "junit")
    }
    api(libs.cql.quick)

    // Test dependencies
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(project(":cqf-fhir-test"))
    testImplementation(libs.slf4j.test)
}
