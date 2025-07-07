package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class ModelArgument {
    @Option(
            names = {"-m", "--model"},
            description = "The model to use for evaluation (e.g. FHIR).")
    public String modelName;

    @Option(
            names = {"-mu", "--model-url"},
            description = "The location of the data to for the model.")
    public String modelUrl;
}
