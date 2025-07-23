package org.opencds.cqf.fhir.cr.cli.command;

import org.opencds.cqf.fhir.cr.cli.version.VersionProvider;
import picocli.CommandLine.Command;

@Command(
        subcommands = {CqlCommand.class, ArgFileCommand.class, MeasureCommand.class},
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
public class CliCommand {}
