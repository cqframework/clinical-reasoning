package org.opencds.cqf.cql.evaluator.cli.command;

import org.opencds.cqf.cql.evaluator.cli.version.VersionProvider;

import picocli.CommandLine.Command;

@Command(subcommands = { CqlCommand.class, ArgFileCommand.class }, mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class CliCommand {
    
}
