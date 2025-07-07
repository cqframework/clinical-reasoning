package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class LibraryArgument {
    @Option(
            names = {"-lu", "--library-url"},
            required = true)
    public String libraryUrl;

    @Option(
            names = {"-ln", "--library-name"},
            required = true)
    public String libraryName;

    @Option(names = {"-lv", "--library-version"})
    public String libraryVersion;

    @Option(names = {"-e", "--expression"})
    public String[] expression;
}
