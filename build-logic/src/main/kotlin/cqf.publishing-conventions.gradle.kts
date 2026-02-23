plugins {
    `java-library`
    `maven-publish`
    signing
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:-missing", "-quiet")
        source = "17"
        isFailOnError = false
    }
}

val isSnapshot = version.toString().endsWith("SNAPSHOT")

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = project.name
                description = "FHIR Clinical Reasoning"
                url = "https://github.com/cqframework/clinical-reasoning"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "cqframework"
                        name = "CQFramework"
                        url = "https://github.com/cqframework"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/cqframework/clinical-reasoning.git"
                    developerConnection = "scm:git:ssh://github.com:cqframework/clinical-reasoning.git"
                    url = "https://github.com/cqframework/clinical-reasoning/tree/master"
                }
            }
        }
    }

    repositories {
        maven {
            name = "central"
            val releasesRepoUrl = uri("https://central.sonatype.com/repository/maven-releases/")
            val snapshotsRepoUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
            url = if (isSnapshot) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = providers.environmentVariable("MAVEN_USERNAME").orNull
                password = providers.environmentVariable("MAVEN_PASSWORD").orNull
            }
        }
    }
}

signing {
    isRequired = !isSnapshot
    sign(publishing.publications["mavenJava"])
}
