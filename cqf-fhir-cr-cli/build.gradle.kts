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

val hapiVersion = project.property("hapi.version")!!.toString()

dependencies {
    api(project(":cqf-fhir-utility"))
    api(project(":cqf-fhir-cql"))
    api(project(":cqf-fhir-cr"))

    api("info.picocli:picocli:4.7.4")
    api("org.slf4j:slf4j-simple:2.0.4")

    // Missing from hapi-bom, hence the need for a version here
    api("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:${hapiVersion}")
    testImplementation(project(":cqf-fhir-test"))
}

description = "FHIR Clinical Reasoning (CLI)"
