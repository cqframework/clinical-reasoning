plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.publish-conventions")
}

dependencies {
    api(project(":cqf-fhir-cr"))
    api(project(":cqf-fhir-utility"))
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.validation)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.converter)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.jpaserver.base)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.storage)
    api("jakarta.annotation:jakarta.annotation-api:2.1.1")
    api("jakarta.servlet:jakarta.servlet-api:5.0.0")
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.server.cds.hooks)
    api(libs.org.springframework.security.spring.security.core)
    testImplementation(libs.ca.uhn.hapi.fhir.hapi.fhir.test.utilities)
    testImplementation(libs.ca.uhn.hapi.fhir.hapi.fhir.storage.test.utilities)
    testImplementation(libs.ca.uhn.hapi.fhir.hapi.fhir.jpaserver.test.utilities)
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (HAPI)"
