plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
    id("cqf.jacoco-conventions")
    id("cqf.publishing-conventions")
    application
    alias(libs.plugins.spring.boot)
}

// No animal-sniffer for this module

application { mainClass = "org.opencds.cqf.fhir.cr.cli.Main" }

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass = "org.opencds.cqf.fhir.cr.cli.Main"
}

// Disable the thin "plain" JAR so the bootJar fat JAR is published as the
// primary unclassified artifact, matching the Maven-era artifact layout.
tasks.named<Jar>("jar") { enabled = false }

// The vanniktech publish plugin derives its publication from components["java"],
// which is wired to the (now-disabled) jar task. Explicitly add the bootJar
// output so the fat JAR is published as the unclassified primary artifact.
afterEvaluate {
    extensions.configure<PublishingExtension> {
        publications.named<MavenPublication>("maven") { artifact(tasks.named("bootJar")) }
    }
}

dependencies {
    implementation(project(":cqf-fhir-utility"))
    implementation(project(":cqf-fhir-cql"))
    implementation(project(":cqf-fhir-cr"))
    implementation(libs.picocli)
    implementation(libs.slf4j.simple)
    implementation(libs.commons.compress)
    implementation(libs.commons.codec)

    runtimeOnly(libs.hapi.fhir.caching.caffeine)

    // Picocli annotation processor
    annotationProcessor(libs.picocli.codegen)

    // Test dependencies
    testImplementation(project(":cqf-fhir-test"))
}

// Picocli compiler argument
tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}
