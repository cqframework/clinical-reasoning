plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":cqf-fhir-cql"))
    api(project(":cqf-fhir-utility"))
    testImplementation(project(":cqf-fhir-test"))
    testImplementation(libs.org.skyscreamer.jsonassert)
    testImplementation(libs.org.mockito.mockito.junit.jupiter)
    testImplementation(libs.org.reflections.reflections)
    testImplementation(libs.com.github.valfirst.slf4j.test)
}

description = "FHIR Clinical Reasoning (Operations)"

val testsJar by tasks.registering(Jar::class) {
    archiveClassifier = "tests"
    from(sourceSets["test"].output)
}

(publishing.publications["maven"] as MavenPublication).artifact(testsJar)
