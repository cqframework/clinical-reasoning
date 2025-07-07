package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class ModelArgument {
    @Option(names = {"-m", "--model"})
    public String modelName;

    @Option(names = {"-mu", "--model-url"})
    public String modelUrl;
}
