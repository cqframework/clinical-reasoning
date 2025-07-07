package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class LibraryArgument {
    @Option(
            names = {"-lu", "--library-url"},
            description = "The location of the CQL to evaluate. This can be a local file path.",
            required = true)
    public String libraryUrl;

    @Option(
            names = {"-ln", "--library-name"},
            description = "The name of the CQL library to evaluate.",
            required = true)
    public String libraryName;

    @Option(
            names = {"-lv", "--library-version"},
            description = "The version of the CQL library to evaluate.")
    public String libraryVersion;

    @Option(
            names = {"-e", "--expression"},
            description = "The CQL expressions within the library to evaluate.")
    public String[] expression;
}
