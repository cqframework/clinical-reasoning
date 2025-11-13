plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    id("jacoco")
    id("org.jetbrains.dokka")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

dependencies {
    // platforms
    implementation(platform("ca.uhn.hapi.fhir:hapi-fhir-bom:8.4.0"))
    testImplementation(platform("org.junit:junit-bom:6.0.1"))

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

tasks.withType<Javadoc>() {
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

jacoco {
    toolVersion = "0.8.11"
}

tasks.test {
    maxHeapSize = "4G"
    configure<JacocoTaskExtension> {
        excludes = listOf("org/hl7/fhir/**", "ca/uhn/fhir/**", "org/cqframework/**")
    }

    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
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

