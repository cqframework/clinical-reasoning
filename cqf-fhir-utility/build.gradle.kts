plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.publish-conventions")
}

dependencies {
    // CQL
    api(libs.org.cqframework.cql.to.elm)
    api(libs.org.cqframework.ucum)
    api(libs.org.cqframework.elm.fhir)
    api(libs.org.cqframework.quick)
    api(libs.org.cqframework.engine)
    api(libs.org.cqframework.engine.fhir)
    api(libs.org.cqframework.cqf.fhir)
    api(libs.org.cqframework.cqf.fhir.npm)

    // HAPI
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.base)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.client)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.dstu2.v1)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.dstu3)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.r4)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.r4b)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.r5)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.validation)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.validation.resources.dstu3)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.validation.resources.r4)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.validation.resources.r4b)
    api(libs.ca.uhn.hapi.fhir.hapi.fhir.validation.resources.r5)
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (Utilities)"