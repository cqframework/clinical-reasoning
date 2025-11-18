import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    id("jacoco")
    id("org.jetbrains.dokka")
    id("io.gitlab.arturbosch.detekt")
    id("buildlogic.publish-conventions")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

val hapiVersion = project.property("hapi.version")!!.toString()
val cqlVersion = project.property("cql.version")!!.toString()
val junitVersion = project.property("junit.version")!!.toString()

dependencies {
    // platforms
    implementation(platform("ca.uhn.hapi.fhir:hapi-fhir-bom:${hapiVersion}"))
    implementation(platform("org.cqframework:cql-bom:${cqlVersion}"))
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))

    // @Nonnull, @Nullable compiler analysis
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // Common dependencies across all projects
    api("org.slf4j:slf4j-api:2.0.4")
    implementation(kotlin("stdlib-jdk8"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

group = "org.opencds.cqf.fhir"
version = project.property("project.version")!!
description = "FHIR Clinical Reasoning Implementations"


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options {
        val standardOptions = this as StandardJavadocDocletOptions
        standardOptions.addStringOption("Xdoclint:none", "-quiet")
        standardOptions.addBooleanOption("html5", true)
        encoding = "UTF-8"
    }
}

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc(), true))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    maxHeapSize = "8G"
    configure<JacocoTaskExtension> {
        includes = listOf("org/opencds/cqf/**")
    }
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
    }
}

jacoco {
    toolVersion = "0.8.13"
}


spotless {
    java {
        targetExclude("**/generated/**")
        palantirJavaFormat("2.38.0")
    }

    kotlin {
        target("**/*.kt")
        targetExclude("**/generated/**", "**/generated-sources/**")
        ktfmt().kotlinlangStyle()
    }
}

detekt {
    buildUponDefaultConfig = true
}

