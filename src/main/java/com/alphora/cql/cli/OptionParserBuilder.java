package com.alphora.cql.cli;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.KeyValuePair;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class OptionParserBuilder {

    public static final String[] HELP_OPTIONS = {"h", "help", "?"};
    public static final String[] LIBRARY_OPTIONS = {"l", "library"};
    public static final String[] LIBRARY_PATH_OPTIONS = {"lp", "library-path"};
    public static final String[] LIBRARY_NAME_OPTIONS = {"ln", "library-name"};
    public static final String[] TERMINOLOGY_URI_OPTIONS = {"t","terminology-uri"};
    public static final String[] MODEL_OPTIONS = {"m","model"};
    public static final String[] PARAMETER_OPTIONS = {"p", "parameter"};
    public static final String[] CONTEXT_PARAMETER_OPTIONS = {"c", "context"};
    public static final String[] EXPRESSION_OPTIONS = {"e", "expression"};

    public static OptionParser build() {
        OptionParser parser = new OptionParser();

        OptionSpec<Void> help = parser.acceptsAll(Arrays.asList(HELP_OPTIONS)).forHelp();
        OptionSpec<String> library = parser.acceptsAll(Arrays.asList(LIBRARY_OPTIONS)).withRequiredArg()
            .describedAs("The library content to evaluate. Use multiple times to define multiple libraries.");

        OptionSpec<String> libraryPath = parser.acceptsAll(Arrays.asList(LIBRARY_PATH_OPTIONS)).availableUnless(library).requiredUnless(library).withRequiredArg()
            .describedAs("The name of the input directory. All files ending in .cql will be processed");

        OptionSpec<String> libraryName = parser.acceptsAll(Arrays.asList(LIBRARY_NAME_OPTIONS)).withRequiredArg()
            .describedAs("The name of the primary library to evaluate. If multiple libraries are defined or a library directory is specified you must specify either the primary library or the expressions you'd like to evaluate.");

        OptionSpec<String> expression = parser.acceptsAll(Arrays.asList(EXPRESSION_OPTIONS)).availableUnless(libraryName).withRequiredArg().ofType(String.class)
            .describedAs("The expressions you'd like to evaluate in the form libraryName.expressionName (e.g. Common.\"Numerator\") Use multiple times to specify multiple expressions. If no expresisons are defined all the expressions of the primary library will be evaluated.");

        // TODO: Terminology user / password (and other auth options)
        OptionSpec<String> terminologyUri = parser.acceptsAll(Arrays.asList(TERMINOLOGY_URI_OPTIONS)).withRequiredArg()
            .describedAs("The URI of the terminology server.");

        // TODO: Data provider user/ password (and other auth options)
        OptionSpec<KeyValuePair> modelUri = parser.acceptsAll(Arrays.asList(MODEL_OPTIONS)).withRequiredArg().ofType(KeyValuePair.class)
            .describedAs("The URI / Path of a data provider for a model in the form of model=uri. (e.g. FHIR=path/to/fhir/resources) Use multiple times to specify multiple models.");

        OptionSpec<KeyValuePair> parameter = parser.acceptsAll(Arrays.asList(PARAMETER_OPTIONS)).withRequiredArg().ofType(KeyValuePair.class)
            .describedAs("The value of a parameter in the form of [libraryName.]parameterName=value. (e.g. Common.\"Measurement Period\"=[@2015-01-01, @2016-01-01]). Libary name is optional. If only the name of the parameter is specified it will be set for all libraries. The parameter values must be CQL types. Use multiple times to specify multiple parameters.");

        OptionSpec<KeyValuePair> context = parser.acceptsAll(Arrays.asList(CONTEXT_PARAMETER_OPTIONS)).withRequiredArg().ofType(KeyValuePair.class)
            .describedAs("The value a context parameter in the form of contextParameterName=value. (e.g. Patient=123) Use multiple times to specify multiple context parameters.");

        return parser;
    }

    public static OptionSet parse(String[] args) {
        return build().parse(args);
    }

    public static InputParameters parseAndConvert(String[] args) {
        OptionSet options = parse(args);

        List<String> libraries = (List<String>)options.valuesOf(LIBRARY_OPTIONS[0]);
        String libraryPath = (String)options.valueOf(LIBRARY_PATH_OPTIONS[0]);
        String libraryName = (String)options.valueOf(LIBRARY_NAME_OPTIONS[0]);
        List<String> expressions = (List<String>)options.valuesOf(EXPRESSION_OPTIONS[0]);

                // This is validation we couldn't define in terms of the jopt API.
        if ((libraries.size() > 1 || libraryPath != null)  && !(libraryName != null || !expressions.isEmpty())){
            new IllegalArgumentException("When more than one library is defined OR a library directory is selected you must define either a primary library or a set of expressions to evaluate.");
        }
    
        String terminologyUri = (String)options.valueOf(TERMINOLOGY_URI_OPTIONS[0]);

        List<KeyValuePair> models = (List<KeyValuePair>)options.valuesOf(MODEL_OPTIONS[0]);
        List<KeyValuePair> parameters = (List<KeyValuePair>)options.valuesOf(PARAMETER_OPTIONS[0]);
        List<KeyValuePair> contextParameters = (List<KeyValuePair>)options.valuesOf(CONTEXT_PARAMETER_OPTIONS[0]);

        InputParameters ip = new InputParameters();
        ip.libraries = libraries;
        ip.libraryPath = libraryPath.toString();
        ip.libraryName = libraryName;
        ip.expressions = toSet(expressions);
        ip.terminologyUri = terminologyUri;
        ip.modelUris = toMap("Model parameters", models);
        ip.parameters = toMap("Parameters", parameters);
        ip.contextParameters = toMap("Context Parameters", contextParameters);

        return ip;

    }

    private static Map<String, String> toMap(String typeOfKeyValuePair, List<KeyValuePair> keyValuePairs) {
        HashMap<String, String> map = new HashMap<>();

        for (KeyValuePair kvp : keyValuePairs) {
            if (map.containsKey(kvp.key)) {
                throw new IllegalArgumentException(String.format("%s contain multiple definitions for %s.", typeOfKeyValuePair, kvp.key));
            }

            map.put(kvp.key, kvp.value);
        }

        return map;
    }

    private static Set<String> toSet(List<String> strings) {
        return strings.stream().distinct().collect(Collectors.toSet());
    }
}