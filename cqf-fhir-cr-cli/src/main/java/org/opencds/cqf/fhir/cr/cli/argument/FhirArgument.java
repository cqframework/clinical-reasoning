package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class FhirArgument {
    @Option(
            names = {"-fv", "--fhir-version"},
            required = true)
    public String fhirVersion;

    @Option(names = {"-rd", "--root-directory"})
    public String rootDirectory;

    @Option(names = {"-ig", "--implementation-guide-path"})
    public String implementationGuidePath;
}
