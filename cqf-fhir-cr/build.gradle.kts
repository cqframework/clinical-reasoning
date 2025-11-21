plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.publish-conventions")
}

dependencies {
    api(project(":cqf-fhir-cql"))
    api(project(":cqf-fhir-utility"))
    testImplementation(project(":cqf-fhir-test"))
    testImplementation("org.skyscreamer:jsonassert:1.5.3")
    testImplementation("com.github.valfirst:slf4j-test:3.0.3")
}

description = "FHIR Clinical Reasoning (Operations)"