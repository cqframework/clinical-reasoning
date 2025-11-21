import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("com.vanniktech.maven.publish")
}

/*
A few things:
   - You must have a Maven Central account (https://central.sonatype.org/register/central-portal/)
   - Your account must have privileges to upload org.cqframework artifacts
   - You must have a gpg key (http://central.sonatype.org/pages/working-with-pgp-signatures.html)
   - You must set your account info and GPG key in your user's gradle.properties file.  For example:
       mavenCentralUsername=foo
       mavenCentralPassword=b@r
       signing.keyId=24875D73
       signing.password=secret
       signing.secretKeyRingFile=/Users/me/.gnupg/secring.gpg
   - If the library version ends with '-SNAPSHOT', it will be deployed to the snapshot repository, else it will be
     deployed to the staging repository (which you then must manually release http://central.sonatype.org/pages/releasing-the-deployment.html).
   - Repo for snapshots for the  modules: https://central.sonatype.com/repository/maven-snapshots/org/opencds/cqf/fhir
   - Repo for releases for the modules: https://central.sonatype.com/repository/maven-releases/org/opencds/cqf/fhir
 */
mavenPublishing {
    publishToMavenCentral(true)
    if (!version.toString().endsWith("SNAPSHOT")) {
        signAllPublications()
    }
    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name = project.name
        description = project.description
        url = "https://github.com/cqframework/clinical-reasoning/tree/master"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        scm {
            connection = "scm:git:git@github.com:cqframework/clinical-reasoning.git"
            developerConnection = "scm:git:git@github.com:cqframework/clinical-reasoning.git"
            url = "git@github.com:cqframework/clinical-reasoning.git"
        }

        developers {
            developer {
                name = "Bryn Rhodes"
            }
            developer {
                name = "Jonathan Percival"
            }
            developer {
                name = "Anton Vasetenkov"
            }
            developer {
                name = "Luke deGruchy"
            }
            developer {
                name = "Chris Schuler"
            }
            developer {
                name = "Justin McKelvy"
            }
        }
    }
}