plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:-missing", "-quiet")
        source = "17"
        isFailOnError = false
    }
}

mavenPublishing {
    publishToMavenCentral(true)
    if (!version.toString().endsWith("SNAPSHOT")) {
        signAllPublications()
    }
    coordinates(project.group.toString(), project.name, project.version.toString())
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
