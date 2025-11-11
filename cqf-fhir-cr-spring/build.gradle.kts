plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":cqf-fhir-cr"))
    api(libs.org.springframework.spring.context)
    testImplementation(libs.org.springframework.spring.test)
}

description = "FHIR Clinical Reasoning (Spring)"
