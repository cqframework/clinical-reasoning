plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "cqf-fhir"

include(
":cqf-fhir-utility",
":cqf-fhir-cr-cli",
":cqf-fhir-cr",
":cqf-fhir-test",
":cqf-fhir-cr-hapi",
":cqf-fhir-benchmark",
":cqf-fhir-cql",
":docs")