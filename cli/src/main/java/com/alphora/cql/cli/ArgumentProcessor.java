package com.alphora.cql.cli;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alphora.cql.service.Parameters;

import org.apache.commons.lang3.tuple.Pair;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import joptsimple.util.KeyValuePair;


public class ArgumentProcessor {

    public static final String[] HELP_OPTIONS = {"h", "help", "?"};
    public static final String[] LIBRARY_OPTIONS = {"l", "library"};
    public static final String[] LIBRARY_PATH_OPTIONS = {"lp", "library-path"};
    public static final String[] LIBRARY_NAME_OPTIONS = {"ln", "library-name"};
    public static final String[] TERMINOLOGY_URI_OPTIONS = {"t","terminology-uri"};
    public static final String[] MODEL_OPTIONS = {"m","model"};
    public static final String[] PARAMETER_OPTIONS = {"p", "parameter"};
    public static final String[] CONTEXT_PARAMETER_OPTIONS = {"c", "context"};
    public static final String[] EXPRESSION_OPTIONS = {"e", "expression"};

    public OptionParser build() {
        OptionParser parser = new OptionParser();

        OptionSpecBuilder libraryBuilder = parser.acceptsAll(asList(LIBRARY_OPTIONS),"Use multiple times to define multiple libraries.");
        OptionSpecBuilder libraryPathBuilder = parser.acceptsAll(asList(LIBRARY_PATH_OPTIONS), "All files ending in .cql will be processed");
        OptionSpecBuilder libraryNameBuilder = parser.acceptsAll(asList(LIBRARY_NAME_OPTIONS), "Required if multiple libraries are defined and --expression is ommitted");        
        OptionSpecBuilder expressionBuilder = parser.acceptsAll(asList(EXPRESSION_OPTIONS), "Use the form libraryName.expressionName. (e.g. Common.\"Numerator\") Use multiple times to specify multiple expressions. If ommitted all the expressions of the primary library will be evaluated.");

        // Set up inter-depedencies.
        parser.mutuallyExclusive(libraryBuilder, libraryPathBuilder);
        parser.mutuallyExclusive(expressionBuilder, libraryNameBuilder);

        OptionSpec<String> library = libraryBuilder.withRequiredArg().describedAs("library content");
        OptionSpec<String> libraryPath = libraryPathBuilder.requiredUnless(library).withRequiredArg().describedAs("input directory for libraries");
        OptionSpec<String> libraryName = libraryNameBuilder.withRequiredArg().describedAs("name of primary library");
        OptionSpec<String> expression = expressionBuilder.withRequiredArg().describedAs("expression to evaluate");

        // TODO: Terminology user / password (and other auth options)
        OptionSpec<String> terminologyUri = parser.acceptsAll(asList(TERMINOLOGY_URI_OPTIONS),"Supports FHIR-based terminology")
            .withRequiredArg().describedAs("uri of terminology server");

        // TODO: Data provider user/ password (and other auth options)
        OptionSpec<KeyValuePair> modelUri = parser.acceptsAll(asList(MODEL_OPTIONS), 
            "Use the form model=uri. (e.g. FHIR=path/to/fhir/resources) Use multiple times to specify multiple models.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("path of data for model ");

        OptionSpec<KeyValuePair> parameter = parser.acceptsAll(asList(PARAMETER_OPTIONS), 
            "Use the form [libraryName.]parameterName=value. (e.g. Common.\"Measurement Period\"=[@2015-01-01, @2016-01-01]). Library name is optional. If ommited parameter will be set for all libraries. The parameter values must be CQL types. Use multiple times to specify multiple parameters.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("value of parameter");

        OptionSpec<KeyValuePair> context = parser.acceptsAll(asList(CONTEXT_PARAMETER_OPTIONS), 
            "Use the form contextParameterName=value. (e.g. Patient=123) Use multiple times to specify multiple context parameters.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("value of context parameter");

        OptionSpec<Void> help = parser.acceptsAll(asList(HELP_OPTIONS), "Show this help page").forHelp();

        return parser;
    }

    public OptionSet parse(String[] args) {
        OptionParser parser = build();
        OptionSet options = parser.parse(args);
        if (options.has(HELP_OPTIONS[0])) {
            try {
                parser.printHelpOn(System.out);
            }
            catch (Exception e) {

            }

            System.exit(0);
        }

        return options;
    }

    public Parameters parseAndConvert(String[] args) {
        OptionSet options = this.parse(args);

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

        Parameters ip = new Parameters();
        ip.libraries = libraries;
        ip.libraryPath = libraryPath;
        ip.libraryName = libraryName;
        ip.expressions = toListOfExpressions(expressions);
        ip.terminologyUri = terminologyUri;
        ip.modelUris = toMap("Model parameters", models);
        ip.parameters = toParameterMap(parameters);
        ip.contextParameters = toMap("Context Parameters", contextParameters);

        return ip;

    }

    private Map<String, String> toMap(String typeOfKeyValuePair, List<KeyValuePair> keyValuePairs) {
        HashMap<String, String> map = new HashMap<>();

        for (KeyValuePair kvp : keyValuePairs) {
            if (map.containsKey(kvp.key)) {
                throw new IllegalArgumentException(String.format("%s contain multiple definitions for %s.", typeOfKeyValuePair, kvp.key));
            }

            map.put(kvp.key, kvp.value);
        }

        return map;
    }

    private List<Pair<String, String>> toListOfExpressions(List<String> strings) {
        List<Pair<String, String>> listOfExpressions = new ArrayList<Pair<String, String>>();

        for (String s : strings) {
            String[] parts = s.split(".");
            if (parts == null || parts.length < 2) {
                new IllegalArgumentException(String.format("%s is not a valid expression. Use the format libraryName.expressionName.", s));
            }

            listOfExpressions.add(Pair.of(parts[0], parts[1]));
        }

        return listOfExpressions;
    }

    // Converts parameters from the CLI format of [libraryName.]parameterName=value to a map. Library name is optional.
    private Map<Pair<String,String>, String> toParameterMap(List<KeyValuePair> keyValuePairs) {
        HashMap<Pair<String,String>, String> map = new HashMap<>();

        for (KeyValuePair kvp : keyValuePairs) {
            String[] parts = kvp.key.split(".");
            map.put(Pair.of(parts.length > 1 ? parts[0] : null, parts.length > 1 ? parts[1] : parts[0]), kvp.value);
        }

        return map;
    }
}