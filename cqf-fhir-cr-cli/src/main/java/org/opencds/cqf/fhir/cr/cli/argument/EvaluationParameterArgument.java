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
        @Option(names = {"-c", "--context"})
        public String contextName;

        @Option(names = {"-cv", "--context-value"})
        public String contextValue;
    }

    static class ParameterArgument {
        @Option(names = {"-p", "--parameter"})
        public String parameterName;

        @Option(names = {"-pv", "--parameter-value"})
        public String parameterValue;
    }
}
