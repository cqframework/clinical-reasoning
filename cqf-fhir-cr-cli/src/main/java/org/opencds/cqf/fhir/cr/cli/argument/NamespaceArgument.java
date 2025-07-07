package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class NamespaceArgument {
    @Option(names = {"-nn", "--namespace-name"})
    public String namespaceName;

    @Option(names = {"-nu", "--namespace-uri"})
    public String namespaceUri;
}
