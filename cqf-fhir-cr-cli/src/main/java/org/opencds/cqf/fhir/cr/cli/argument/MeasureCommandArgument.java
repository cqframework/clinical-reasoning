package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class MeasureCommandArgument {

    @ArgGroup(multiplicity = "1", exclusive = false)
    public CqlCommandArgument cql;

    @Option(
            names = {"--measure"},
            required = true,
            description = "Provides the name of the measure to evaluate.")
    public String measureName;

    @Option(
            names = {"--period-start"},
            description = "Specifies the start of the evaluation period.")
    public String periodStart;

    @Option(
            names = {"--period-end"},
            description = "Specifies the end of the evaluation period.")
    public String periodEnd;

    @Option(
            names = {"--measure-path"},
            required = true,
            description = "Specifies the path to the measure resource.")
    public String measurePath;

    @Option(
            names = {"--report-path"},
            description = "Specifies the path to the report output directory.")
    public String reportPath;
}
