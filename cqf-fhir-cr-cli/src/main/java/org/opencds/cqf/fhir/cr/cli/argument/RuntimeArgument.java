package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class RuntimeArgument {
    @Option(
            names = {"--enable-hedis-compatibility-mode"},
            description =
                    "Enable HEDIS compatibility mode, which changes the evaluation behavior to match HEDIS expectations (as of 2025).")
    public boolean hedisCompatibilityMode;
}
