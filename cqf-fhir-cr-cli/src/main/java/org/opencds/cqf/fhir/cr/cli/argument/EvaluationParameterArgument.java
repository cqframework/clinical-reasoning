package org.opencds.cqf.fhir.cr.cli.argument;

import java.util.List;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class EvaluationParameterArgument {
    @ArgGroup(multiplicity = "0..*", exclusive = false)
    public List<ParameterArgument> parameters;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public ContextArgument context;

    public static class ContextArgument {
        @Option(
                names = {"-c", "--context"},
                description = "Specifies the CQL context to set (e.g Patient, Encounter).")
        public String contextName;

        @Option(
                names = {"-cv", "--context-value"},
                description = "Specifies the CQL context value (e.g. Patient ID, Encounter ID).")
        public String contextValue;
    }

    static class ParameterArgument {
        @Option(
                names = {"-p", "--parameter"},
                description = "Specifies the name of CQL parameter to set.")
        public String parameterName;

        @Option(
                names = {"-pv", "--parameter-value"},
                description = "Specifies the value of CQL parameter to set.")
        public String parameterValue;
    }
}
