package org.opencds.cqf.fhir.cr.cli.argument;

import picocli.CommandLine.Option;

public class NamespaceArgument {
    @Option(
            names = {"-nn", "--namespace-name"},
            description = "A CQL namespace to map to a URI (e.g. cqf.commmon).")
    public String namespaceName;

    @Option(
            names = {"-nu", "--namespace-uri"},
            description = "The URI to map the CQL namespace to (e.g. http://common.cqf.org).")
    public String namespaceUri;
}
