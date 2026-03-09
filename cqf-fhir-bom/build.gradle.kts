plugins {
    `java-platform`
    id("com.vanniktech.maven.publish")
}

// group and version are set automatically from gradle.properties

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":cqf-fhir-test"))
        api(project(":cqf-fhir-utility"))
        api(project(":cqf-fhir-cql"))
        api(project(":cqf-fhir-cr"))
        api(project(":cqf-fhir-cr-cli"))
        api(project(":cqf-fhir-cr-hapi"))
        api(project(":cqf-fhir-cr-spring"))
    }
}

mavenPublishing {
    publishToMavenCentral(true)
    if (!version.toString().endsWith("SNAPSHOT")) {
        signAllPublications()
    }
    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name = "FHIR Clinical Reasoning (Bill Of Materials)"
        description = "FHIR Clinical Reasoning BOM"
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
