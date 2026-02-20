plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
}

// No publishing for docs module

dependencies {
    implementation(project(":cqf-fhir-cql"))
    implementation(project(":cqf-fhir-cr"))
    implementation(project(":cqf-fhir-test"))
    implementation(project(":cqf-fhir-utility"))
}
