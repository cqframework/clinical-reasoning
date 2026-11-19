import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    alias(libs.plugins.errorprone)
    checkstyle
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

repositories {
    if (file("${rootProject.projectDir}/local.properties").exists()) {
        mavenLocal()
    }
    mavenCentral()
    maven {
        name = "central-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    api(platform(libs.hapi.fhir.bom))
    api(platform(libs.cql.bom))
    // Align Jackson modules at a patched security version.
    api(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    testImplementation(platform(libs.junit.bom))

    compileOnly(libs.jakarta.annotation.api)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.hamcrest)
    testImplementation(libs.mockito.core)
    testImplementation(libs.equalsverifier)

    errorprone("com.google.errorprone:error_prone_core:${libs.versions.error.prone.core.get()}")
}

// Change distributed/test Guava only; retain upstream compile-time annotations/API.
configurations.matching { it.name == "runtimeClasspath" || it.name.endsWith("RuntimeClasspath") }
    .configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.guava" && requested.name == "guava") {
                useVersion("33.7.2-jre")
                because("Address fixable HIGH security findings in the distributed Guava runtime")
            }
        }
    }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.errorprone {
        enabled = true
        disableAllChecks = true
    }
    options.isFork = true
    options.forkOptions.jvmArgs =
        listOf(
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-opens",
            "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-opens",
            "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        )
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("config/checkstyle.xml")
    isIgnoreFailures = false
}

tasks.named("checkstyleTest") { enabled = false }

// Fix capability conflict between guava and google-collections in checkstyle classpath
configurations.named("checkstyle") {
    resolutionStrategy.capabilitiesResolution.withCapability(
        "com.google.collections:google-collections"
    ) {
        select("com.google.guava:guava:0")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "2g"
    jvmArgs(
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util=ALL-UNNAMED",
    )
}

tasks.named<Test>("test") { exclude("**/*IT.class") }

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    useJUnitPlatform()
    include("**/*IT.class")
    shouldRunAfter(tasks.named("test"))
}

val projectName = project.name
val projectVersion = project.version

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to projectName,
            "Implementation-Version" to projectVersion,
            "Specification-Title" to projectName,
            "Specification-Version" to projectVersion,
        )
    }
}
