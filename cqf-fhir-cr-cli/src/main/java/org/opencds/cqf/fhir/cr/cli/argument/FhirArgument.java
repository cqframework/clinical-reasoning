package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class FhirArgument {
    @Option(
            names = {"-fv", "--fhir-version"},
            defaultValue = "R4",
            description = "The FHIR version to use for evaluation (e.g. R4, R5).")
    public String fhirVersion;

    // TODO: Given a well-defined CQL directory structure, we ought to be able to infer the
    // paths to the terminology and data and IG files automatically.
    @Option(
            names = {"-root", "--root-directory"},
            description =
                    "The root directory for the FHIR resources. This is used to resolve relative paths in the implementation guide.")
    public String rootDirectory;

    @Option(
            names = {"-ig", "--implementation-guide-path"},
            description = "The path to the FHIR implementation guide.")
    public String implementationGuidePath;

    @Option(
            names = {"-data", "--data-url"},
            description = "The location of the data to use for evaluation. Can be a local file path.")
    public String dataUrl;

    @Option(
            names = {"-terminology", "--terminology-url"},
            description =
                    "Specifies the location of the terminology to be used for CQL evaluation. Can be a local file path.")
    public String terminologyUrl;
}
