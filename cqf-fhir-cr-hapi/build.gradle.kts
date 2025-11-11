plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.validation)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.converter)
    api(project(":cqf-fhir-cr"))
    api(project(":cqf-fhir-utility"))
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.jpaserver.base)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.storage)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.server.cds.hooks)
    api(libs.org.springframework.security.spring.security.core)
    testImplementation(libs.ca.uhn.hapi.fhir.hapi.fhir.test.utilities)
    testImplementation(libs.ca.uhn.hapi.fhir.hapi.fhir.storage.test.utilities)
    testImplementation(libs.ca.uhn.hapi.fhir.hapi.fhir.jpaserver.test.utilities)
    testImplementation(project(":cqf-fhir-test"))
    testImplementation(libs.org.mockito.mockito.junit.jupiter)
    testImplementation(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.dstu2.v1)
}

description = "FHIR Clinical Reasoning (HAPI)"
