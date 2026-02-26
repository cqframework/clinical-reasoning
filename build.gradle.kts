plugins {
    alias(libs.plugins.sonarqube)
    jacoco
    id("cqf.ci-conventions")
}

// Required for the JaCoCo ant dependency used by the aggregate report task
repositories {
    mavenCentral()
}

sonar {
    properties {
        property("sonar.organization", "cqframework")
        property("sonar.projectKey", "cqframework_clinical-reasoning")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.java.source", "17")
        property("sonar.java.target", "17")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get()}/reports/jacoco/jacocoAggregateReport/jacocoAggregateReport.xml")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.exclusions", "**/generated/**,**/benchmark/**")
        property("sonar.coverage.exclusions", "**/config/**,**/constants/**")
    }
}

// Ensure the sonar task generates the aggregate JaCoCo report first so coverage data is available
tasks.named("sonar") {
    dependsOn("jacocoAggregateReport")
}

// Aggregate JaCoCo reports from all subprojects that apply the jacoco plugin
tasks.register<JacocoReport>("jacocoAggregateReport") {
    group = "verification"
    description = "Generates an aggregate JaCoCo report across all subprojects."

    val jacocoSubprojects = subprojects.filter { it.plugins.hasPlugin("jacoco") }
    dependsOn(jacocoSubprojects.map { it.tasks.named("test") })

    sourceDirectories.setFrom(jacocoSubprojects.map { it.the<SourceSetContainer>()["main"].allSource.srcDirs })
    classDirectories.setFrom(jacocoSubprojects.map { it.the<SourceSetContainer>()["main"].output })
    executionData.setFrom(jacocoSubprojects.map {
        it.tasks.named<Test>("test").map { task ->
            task.extensions.getByType<JacocoTaskExtension>().destinationFile!!
        }
    })

    reports {
        xml.required = true
        html.required = true
    }
}
