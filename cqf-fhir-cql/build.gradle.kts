plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.publish-conventions")
}

dependencies {
    api(project(":cqf-fhir-utility"))
    api(libs.org.cqframework.cql.to.elm)
    api(libs.org.cqframework.ucum)
    api(libs.org.cqframework.elm.fhir)
    api(libs.org.cqframework.quick)
    api(libs.org.cqframework.engine)
    api(libs.org.cqframework.engine.fhir)
    api(libs.org.cqframework.cqf.fhir)
    api(libs.org.cqframework.cqf.fhir.npm)
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (CQL)"
