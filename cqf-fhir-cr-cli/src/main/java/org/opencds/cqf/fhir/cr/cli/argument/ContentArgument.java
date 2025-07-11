package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class ContentArgument {
    @Option(
            names = {"-source", "--source-path"},
            description = "The root directory of the CQL files.",
            required = true)
    public String cqlPath;

    @Option(
            names = {"-name", "--library-name"},
            description = "The name of the CQL library to evaluate.",
            required = true)
    public String name;

    @Option(
            names = {"-e", "--expression"},
            description = "The CQL expressions within the library to evaluate.")
    public String[] expression;
}
