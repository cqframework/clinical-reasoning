plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":cqf-fhir-cr"))
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (Benchmarks)"
