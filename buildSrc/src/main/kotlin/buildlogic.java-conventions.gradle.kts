plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    `java-library`
    `maven-publish`
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

    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.15.6")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
version = "4.0.0-SNAPSHOT"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    maxHeapSize = "4g"
    useJUnitPlatform()
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
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
