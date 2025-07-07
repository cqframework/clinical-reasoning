package org.opencds.cqf.fhir.cr.cli.argument;

import java.util.List;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

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

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public RuntimeArgument runtime;

    @Option(names = {"-op", "--output-path"})
    public String outputPath;

    @Option(names = {"-t", "--terminology-url"})
    public String terminologyUrl;
}
