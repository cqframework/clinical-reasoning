package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Arguments required for the `cql` command
 */
public class CqlCommandArgument {
    @ArgGroup(multiplicity = "1", exclusive = false)
    public FhirArgument fhir;

    @ArgGroup(multiplicity = "1", exclusive = false)
    public ContentArgument content;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public NamespaceArgument namespace;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public ParametersArgument parameters;

    @Option(
            names = {"-hedis", "--enable-hedis-compatibility-mode"},
            description =
                    "Enable HEDIS compatibility mode, which changes the CQL evaluation behavior to match HEDIS expectations (as of 2025).")
    public boolean hedisCompatibilityMode = false;

    @Option(
            names = {"-op", "--output-path"},
            description = "Specifies the path where the CQL output files will be written.")
    public String outputPath;
}
