import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.publish-conventions")
    id("org.springframework.boot") version "3.3.5"
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass = "org.opencds.cqf.cql.cli.Main"
}

mavenPublishing {
   publishing {
       publications {
           withType<MavenPublication> {
               artifact(tasks.getByName("bootJar"))
           }
       }
   }
}

dependencies {
    api(project(":cqf-fhir-utility"))
    api(project(":cqf-fhir-cql"))
    api(project(":cqf-fhir-cr"))
    api(libs.info.picocli.picocli)
    api(libs.org.slf4j.slf4j.simple)
    api(libs.org.apache.commons.commons.compress)
    api(libs.commons.codec.commons.codec)
    runtimeOnly(libs.ca.uhn.hapi.fhir.hapi.fhir.caching.caffeine)
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (CLI)"
