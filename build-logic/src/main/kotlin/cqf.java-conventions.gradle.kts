import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    id("net.ltgt.errorprone")
    checkstyle
}

val catalog: VersionCatalog = versionCatalogs.named("libs")
fun lib(name: String) = catalog.findLibrary(name).get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven {
        name = "central-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
}

dependencies {
    api(platform(lib("hapi-fhir-bom")))
    api(platform(lib("cql-bom")))
    testImplementation(platform(lib("junit-bom")))

    compileOnly(lib("jakarta-annotation-api"))
    implementation(lib("slf4j-api"))

    testImplementation(lib("junit-jupiter"))
    testRuntimeOnly(lib("junit-platform-launcher"))
    testImplementation(lib("hamcrest"))
    testImplementation(lib("mockito-core"))
    testImplementation(lib("equalsverifier"))

    errorprone("com.google.errorprone:error_prone_core:${BuildConfig.ERROR_PRONE}")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.errorprone {
        enabled = true
        disableAllChecks = true
    }
    options.isFork = true
    options.forkOptions.jvmArgs = listOf(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
    )
}

checkstyle {
    toolVersion = BuildConfig.CHECKSTYLE
    configFile = rootProject.file("config/checkstyle.xml")
    isIgnoreFailures = false
}

tasks.named("checkstyleTest") {
    enabled = false
}

// Fix capability conflict between guava and google-collections in checkstyle classpath
configurations.named("checkstyle") {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
        select("com.google.guava:guava:0")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "2g"
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED"
    )
}

tasks.named<Test>("test") {
    exclude("**/*IT.class")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    useJUnitPlatform()
    include("**/*IT.class")
    shouldRunAfter(tasks.named("test"))
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Specification-Title" to project.name,
            "Specification-Version" to project.version
        )
    }
}
