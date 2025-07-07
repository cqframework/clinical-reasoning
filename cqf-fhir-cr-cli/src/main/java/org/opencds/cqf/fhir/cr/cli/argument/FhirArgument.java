package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class FhirArgument {
    @Option(
            names = {"-fv", "--fhir-version"},
            defaultValue = "R4",
            description = "The FHIR version to use for evaluation (e.g. R4, R5).")
    public String fhirVersion;

    @Option(
            names = {"-rd", "--root-directory"},
            description =
                    "The root directory for the FHIR resources. This is used to resolve relative paths in the implementation guide.")
    public String rootDirectory;

    @Option(
            names = {"-ig", "--implementation-guide-path"},
            description = "The path to the FHIR implementation guide.")
    public String implementationGuidePath;
}
