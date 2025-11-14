plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":cqf-fhir-cr"))
    testImplementation(project(":cqf-fhir-test"))

    api("org.openjdk.jmh:jmh-core:1.37")
    api("org.skyscreamer:jsonassert:1.5.3")
}

description = "FHIR Clinical Reasoning (Benchmarks)"
