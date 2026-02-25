import net.ltgt.gradle.errorprone.errorprone

// No jacoco, no animal-sniffer, no publishing for benchmarks
plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
}



dependencies {
    implementation(project(":cqf-fhir-cr"))
    implementation(project(":cqf-fhir-test"))
    implementation(libs.jmh.core)

    // JMH annotation processor
    annotationProcessor(libs.jmh.generator.annprocess)

    testImplementation(libs.jsonassert)
    testImplementation(libs.slf4j.simple)
}

// Disable Error Prone for benchmarks and use JMH-specific compiler args
tasks.withType<JavaCompile>().configureEach {
    options.errorprone.enabled = false
    options.compilerArgs = mutableListOf("-implicit:class")
}

// Benchmarks have no JUnit tests, only JMH benchmarks
tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}
