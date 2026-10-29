import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("cqf.java-conventions")
    id("cqf.jacoco-conventions")
    id("cqf.animal-sniffer-conventions")
    id("cqf.publishing-conventions")
}

dependencies {
    api(project(":cqf-fhir-cql"))
    api(project(":cqf-fhir-utility"))

    // Test dependencies
    testImplementation(project(":cqf-fhir-test"))
    testImplementation(libs.jsonassert)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.reflections)
    testImplementation(libs.hamcrest)
    testImplementation(libs.slf4j.test)
}

// Test JAR for downstream modules
val testJar by
    tasks.registering(Jar::class) {
        archiveClassifier = "tests"
        from(sourceSets.test.get().output)
    }

configurations { create("testArtifacts") { extendsFrom(configurations.testImplementation.get()) } }

artifacts { add("testArtifacts", testJar) }

// Processed test resources as an on-disk directory for downstream modules that load
// IG fixtures from the filesystem (e.g. cqf-fhir-benchmark). Consumers get a task
// dependency on processTestResources, so build ordering is handled by Gradle.
val testResourcesElements by
    configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = false
    }

artifacts {
    add(testResourcesElements.name, layout.buildDirectory.dir("resources/test")) {
        builtBy(tasks.processTestResources)
    }
}

// Add test JAR to publishing
afterEvaluate {
    publishing { publications { named<MavenPublication>("maven") { artifact(testJar) } } }
}

// Generate build properties
val generateBuildProperties by
    tasks.registering {
        val outputDir = layout.buildDirectory.dir("generated-sources/properties")
        val propsFile =
            outputDir.map { it.file("org/opencds/cqf/fhir/cqf-fhir-cr-build.properties") }
        val projectVersion = project.version.toString()

        outputs.dir(outputDir)

        doLast {
            val gitCommit =
                try {
                    providers
                        .exec { commandLine("git", "rev-parse", "HEAD") }
                        .standardOutput
                        .asText
                        .get()
                        .trim()
                } catch (_: Exception) {
                    "UNKNOWN"
                }

            val gitBranch =
                try {
                    providers
                        .exec { commandLine("git", "rev-parse", "--abbrev-ref", "HEAD") }
                        .standardOutput
                        .asText
                        .get()
                        .trim()
                } catch (_: Exception) {
                    "UNKNOWN"
                }

            val timestamp =
                OffsetDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SXXX"))

            val timestampRegex = Regex("clinicalreasoning\\.timestamp=.*\\n")
            val content = buildString {
                appendLine("clinicalreasoning.buildnumber=$gitCommit")
                appendLine("clinicalreasoning.timestamp=$timestamp")
                appendLine("clinicalreasoning.version=$projectVersion")
                appendLine("scmBranch=$gitBranch")
            }

            // Only write if meaningful content changed (ignoring the timestamp)
            // to avoid triggering IDE file-watcher rebuild loops.
            val file = propsFile.get().asFile
            file.parentFile.mkdirs()
            if (file.exists()) {
                val existing = file.readText()
                if (existing.replace(timestampRegex, "") == content.replace(timestampRegex, "")) {
                    return@doLast
                }
            }
            file.writeText(content)
        }
    }

sourceSets.main { resources.srcDir(generateBuildProperties.map { it.outputs.files.singleFile }) }

tasks.named("processResources") { dependsOn(generateBuildProperties) }
