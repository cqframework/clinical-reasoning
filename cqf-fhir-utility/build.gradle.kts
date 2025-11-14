plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.publish-conventions")
}

dependencies {
    // CQL
    api("org.cqframework:cql-to-elm")
    api("org.cqframework:elm-fhir")
    api("org.cqframework:ucum")
    api("org.cqframework:engine-fhir")
    api("org.cqframework:quick")
    api("org.cqframework:cqf-fhir")
    api("org.cqframework:cqf-fhir-npm")

    // HAPI
    api("ca.uhn.hapi.fhir:hapi-fhir-base")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-dstu2.1")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-dstu3")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-r4")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-r5")
    api("ca.uhn.hapi.fhir:hapi-fhir-validation")
    api("ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4")
    api("ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r5")
    api("ca.uhn.hapi.fhir:hapi-fhir-client")

    testImplementation(project(":cqf-fhir-test"))
    testImplementation("com.github.valfirst:slf4j-test:3.0.3")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.15.6")
}

description = "FHIR Clinical Reasoning (Utilities)"