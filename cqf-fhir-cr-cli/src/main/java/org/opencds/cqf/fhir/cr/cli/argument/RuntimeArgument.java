package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class RuntimeArgument {
    @Option(names = {"--enable-hedis-compatibility-mode"})
    public boolean hedisCompatibilityMode;
}
