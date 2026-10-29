import net.ltgt.gradle.errorprone.errorprone

// No jacoco, no animal-sniffer, no publishing for benchmarks
plugins { id("cqf.java-conventions") }

dependencies {
    implementation(project(":cqf-fhir-cr"))
    implementation(project(":cqf-fhir-test"))
    implementation(libs.jmh.core)

    // Benchmarks live in the test source set, so JMH must process test sources
    testAnnotationProcessor(libs.jmh.generator.annprocess)

    // On-disk IG fixtures (measures, plan definitions, questionnaires, terminology)
    testRuntimeOnly(project(path = ":cqf-fhir-cr", configuration = "testResourcesElements"))

    testImplementation(libs.jsonassert)
    testImplementation(libs.slf4j.simple)
}

// Disable Error Prone for benchmarks and use JMH-specific compiler args
tasks.withType<JavaCompile>().configureEach {
    options.errorprone.enabled = false
    options.compilerArgs = mutableListOf("-implicit:class")
}

// Benchmarks never run as part of test/integrationTest/build; they are compiled and
// linted only. Use the jmh task below to run them on demand.
tasks.withType<Test>().configureEach { enabled = false }

// Runs the JMH benchmarks on demand. Pass JMH command line options with -PjmhArgs, e.g.
//   ./gradlew :cqf-fhir-benchmark:jmh -PjmhArgs="Measures -f 2 -wi 3 -i 5"
// Results are written to build/reports/jmh/results.json.
val jmhArgs = providers.gradleProperty("jmhArgs")
val jmhResults = layout.buildDirectory.file("reports/jmh/results.json")

tasks.register<JavaExec>("jmh") {
    description = "Runs the JMH benchmarks."
    group = "benchmark"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "org.openjdk.jmh.Main"
    outputs.upToDateWhen { false }
    argumentProviders.add {
        val results = jmhResults.get().asFile
        results.parentFile.mkdirs()
        jmhArgs.map { it.split(" ").filter(String::isNotBlank) }.getOrElse(emptyList()) +
            listOf("-rf", "json", "-rff", results.absolutePath)
    }
}
