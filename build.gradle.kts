plugins {
    id("org.sonarqube") version "7.0.1.6134"
}

sonar {
    properties {
        property("sonar.projectKey", "clinical-reasoning")
        property("sonar.organization", "cqframework")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}