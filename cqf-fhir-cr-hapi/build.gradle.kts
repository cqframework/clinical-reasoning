plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
    id("cqf.jacoco-conventions")
    id("cqf.animal-sniffer-conventions")
    id("cqf.publishing-conventions")
}

dependencies {
    api(libs.hapi.fhir.validation)
    api(libs.hapi.fhir.converter)
    api(project(":cqf-fhir-cr"))
    api(project(":cqf-fhir-utility"))
    api(libs.hapi.fhir.jpaserver.base) {
        exclude(group = "org.glassfish", module = "jakarta.json")
    }
    api(libs.hapi.fhir.storage) {
        exclude(group = "com.sun.activation", module = "jakarta.activation")
    }
    api(libs.hapi.fhir.server.cds.hooks) {
        exclude(group = "junit", module = "junit")
        exclude(group = "com.sun.activation", module = "jakarta.activation")
    }
    implementation(libs.spring.security.core)
    compileOnly(libs.jakarta.servlet.api)

    // Test dependencies
    testImplementation(libs.hapi.fhir.test.utilities)
    testImplementation(libs.hapi.fhir.storage.test.utilities)
    testImplementation(libs.hapi.fhir.jpaserver.test.utilities)
    testImplementation(project(":cqf-fhir-test"))
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.hapi.fhir.structures.dstu2x1)
}
