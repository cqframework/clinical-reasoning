plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.publish-conventions")
}

dependencies {
    api(project(":cqf-fhir-utility"))
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (CQL)"
