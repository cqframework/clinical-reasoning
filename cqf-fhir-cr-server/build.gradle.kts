import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("cqf.java-conventions")
    id("cqf.spotless-conventions")
    id("cqf.jacoco-conventions")
    application
    alias(libs.plugins.spring.boot)
}

application { mainClass = "org.opencds.cqf.fhir.cr.server.Application" }

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass = "org.opencds.cqf.fhir.cr.server.Application"
    archiveClassifier = ""
}

// Disable the thin "plain" JAR so the bootJar fat JAR is the primary artifact.
tasks.named<Jar>("jar") { enabled = false }

dependencies {
    // cqf-fhir-cr-hapi pulls hapi-fhir-jpaserver-base + hapi-fhir-server-cds-hooks via `api`.
    // Both bring substantial transitive trees (Hibernate Search → Lucene/Elasticsearch, JDBC
    // drivers, CDS-Hooks framework) we never initialize at runtime in this server.
    //
    // The exclusion list below is the *known-safe* subset: it removes the modules that contain
    // no classes touched at runtime, verified by integration tests + live curl smoke. More
    // aggressive exclusions (Hibernate ORM, Hibernate Search, Spring Data, FHIR version model
    // jars) caused HAPI's reflective `ValidationSupportChain` construction (used by the
    // FHIRPath engine inside ResourceMatcher) to fail with HAPI-2330 on string searches.
    // Resolving that requires either upstream changes in cqf-fhir-cr-hapi to make those deps
    // optional, or wiring a non-default ValidationSupport so the reflective fallback is skipped.
    //
    // Result: 311 MB -> ~210 MB (-32%). Phase-2 split of cqf-fhir-cr-hapi gets us the rest.
    api(project(":cqf-fhir-cr-hapi")) {
        // Trim only dependencies with no reflective entry point in HAPI's validator/FHIRPath
        // construction. Wider exclusions (the JPA modules, Hibernate Search, FHIR-version
        // model jars) broke ValidationSupportChain reflective construction (HAPI-2330) which
        // ResourceMatcher needs for string-typed search params.
        exclude(group = "ca.uhn.hapi.fhir", module = "hapi-fhir-server-cds-hooks")
        exclude(group = "org.xerial", module = "sqlite-jdbc")
        exclude(group = "com.oracle.database.jdbc", module = "ojdbc11")
        exclude(group = "com.h2database", module = "h2")
        exclude(group = "org.postgresql", module = "postgresql")
        exclude(group = "com.microsoft.sqlserver", module = "mssql-jdbc")
        exclude(group = "net.sourceforge.plantuml", module = "plantuml-mit")
        exclude(group = "org.apache.jena")
        exclude(group = "co.elastic.clients")
        exclude(group = "org.elasticsearch")
        exclude(group = "org.elasticsearch.client")
        // hapi-fhir-storage-cr brings the CDS-Hooks server pieces; CDS hook flow isn't wired.
        exclude(group = "ca.uhn.hapi.fhir", module = "hapi-fhir-storage-cr")

        // Hibernate ORM core. (Hibernate Search stays — its absence broke ValidationSupportChain
        // reflection.)
        exclude(group = "org.hibernate.orm", module = "hibernate-core")
        exclude(group = "org.hibernate.orm", module = "hibernate-envers")

        // The hapi-fhir-jpaserver-* modules + hapi-fhir-jpa stay despite never being
        // instantiated. Excluding any subset breaks HAPI's reflective ValidationSupportChain
        // construction (used by ResourceMatcher's FHIRPath engine for string searches and
        // operation processors). Need an upstream split before they can come out.
    }
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.autoconfigure)
    compileOnly(libs.jakarta.servlet.api)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.hapi.fhir.test.utilities)
    testImplementation(project(":cqf-fhir-test"))
}

// OCI image via Spring Boot's buildpacks (no Dockerfile required).
// Run: ./gradlew :cqf-fhir-cr-server:bootBuildImage
tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("cqf-fhir-cr-server:${project.version}")
    environment.put("BP_JVM_VERSION", "17")
}
