plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
    id("cqf.jacoco-conventions")
    id("cqf.animal-sniffer-conventions")
    id("cqf.publishing-conventions")
}

dependencies {
    // CQL Engine - api scope since types are used by downstream modules
    api(libs.cql.engine)
    api(libs.cql.engine.fhir) {
        exclude(group = "junit", module = "junit")
    }
    api(libs.cql.to.elm.jvm)
    api(libs.cql.ucum)
    api(libs.cql.elm.fhir)

    // HAPI FHIR structures - api scope since types are used by downstream modules
    api(libs.hapi.fhir.base)
    api(libs.hapi.fhir.client)
    api(libs.hapi.fhir.structures.dstu3)
    api(libs.hapi.fhir.structures.r4)
    api(libs.hapi.fhir.structures.r4b)
    api(libs.hapi.fhir.structures.r5)

    // HAPI FHIR validation resources
    api(libs.hapi.fhir.validation)
    api(libs.hapi.fhir.validation.resources.dstu3)
    api(libs.hapi.fhir.validation.resources.r4)
    api(libs.hapi.fhir.validation.resources.r4b)
    api(libs.hapi.fhir.validation.resources.r5)

    // Test dependencies
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(project(":cqf-fhir-test"))
    testImplementation(libs.slf4j.test)
}
