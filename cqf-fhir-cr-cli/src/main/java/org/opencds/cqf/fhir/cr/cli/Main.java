package org.opencds.cqf.fhir.cr.cli;

import java.util.Objects;
import org.opencds.cqf.fhir.cr.cli.command.CliCommand;
import picocli.CommandLine;

// CHECKSTYLE.OFF: all
public class Main {

    public static void main(String[] args) {
        int exitCode = run(args);
        System.exit(exitCode);
    }

    public static int run(String[] args) {
        Objects.requireNonNull(args);
        CommandLine cli = new CommandLine(new CliCommand());
        return cli.execute(args);
    }
}
