package org.opencds.cqf.fhir.cr.cli.argument;

import java.util.List;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * Arguments required for the `cql` command
 */
public class CqlArgument {
    @ArgGroup(multiplicity = "1..1", exclusive = false)
    public FhirArgument fhir;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public NamespaceArgument namespace;

    @ArgGroup(multiplicity = "1..1", exclusive = false)
    public LibraryArgument library;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public ModelArgument model;

    @ArgGroup(multiplicity = "0..*", exclusive = false)
    public List<EvaluationParameterArgument> evaluation;

    @Option(
            names = {"--enable-hedis-compatibility-mode"},
            required = false,
            defaultValue = "false",
            description =
                    "Enable HEDIS compatibility mode, which changes the CQL evaluation behavior to match HEDIS expectations (as of 2025).")
    public boolean hedisCompatibilityMode;

    @Option(
            names = {"-op", "--output-path"},
            description = "Specifies the path where the CQL output files will be written.")
    public String outputPath;

    @Option(
            names = {"-t", "--terminology-url"},
            description = "Specifies the location of the terminology to be used for CQL evaluation.")
    public String terminologyUrl;
}
