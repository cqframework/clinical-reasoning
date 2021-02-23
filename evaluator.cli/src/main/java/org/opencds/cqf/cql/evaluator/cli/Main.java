package org.opencds.cqf.cql.evaluator.cli;

import java.util.Objects;

import org.opencds.cqf.cql.evaluator.cli.command.CliCommand;

import picocli.CommandLine;

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