plugins {
    id("org.sonarqube") version "7.0.1.6134"
}

sonar {
    properties {
        property("sonar.projectKey", "cqframework_clinical-reasoning")
        property("sonar.organization", "cqframework")
    }
}