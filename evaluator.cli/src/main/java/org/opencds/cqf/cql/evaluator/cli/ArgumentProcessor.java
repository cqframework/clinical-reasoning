package org.opencds.cqf.cql.evaluator.cli;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import org.opencds.cqf.cql.evaluator.ExpressionInfo;
import org.hl7.elm_modelinfo.r1.ModelInfo;
//import org.opencds.cqf.cql.evaluator.ParameterInfo;
import org.opencds.cqf.cql.evaluator.builder.BuilderParameters;

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
    public static final String[] LIBRARY_VERSION_OPTIONS = {"lv", "library-version"};

    public static final String[] MEASURE_PATH_OPTIONS = { "mp", "measure-path"};
    public static final String[] MEASURE_NAME_OPTIONS = {"mn", "measure-name"};
    public static final String[] MEASURE_VERSION_OPTIONS = {"mv", "measure-version"};

    public static final String[] TERMINOLOGY_URI_OPTIONS = {"t","terminology-uri"};
    public static final String[] MODEL_OPTIONS = {"m","model"};
    public static final String[] PARAMETER_OPTIONS = {"p", "parameter"};
    public static final String[] CONTEXT_PARAMETER_OPTIONS = {"c", "context"};
    public static final String[] EXPRESSION_OPTIONS = {"e", "expression"};
    public static final String[] OUTPUT_FORMAT_OPTIONS = {"v", "verbose"};

    public OptionParser build() {
        OptionParser parser = new OptionParser();

        OptionSpecBuilder libraryBuilder = parser.acceptsAll(asList(LIBRARY_OPTIONS),"Use multiple times to define multiple libraries.");
        
        OptionSpecBuilder libraryPathBuilder = parser.acceptsAll(asList(LIBRARY_PATH_OPTIONS), "All files ending in .cql will be processed");
        OptionSpecBuilder libraryNameBuilder = parser.acceptsAll(asList(LIBRARY_NAME_OPTIONS), "Required if multiple libraries are defined and --expression is omitted");
        OptionSpecBuilder libraryVersionBuilder = parser.acceptsAll(asList(LIBRARY_VERSION_OPTIONS), "If omitted most recent version of the library will be used");     
        
        OptionSpecBuilder expressionBuilder = parser.acceptsAll(asList(EXPRESSION_OPTIONS), "Use the form libraryName.expressionName. (e.g. Common.\"Numerator\") Use multiple times to specify multiple expressions. If omitted all the expressions of the primary library will be evaluated.");

        OptionSpecBuilder measurePathBuilder = parser.acceptsAll(asList(MEASURE_PATH_OPTIONS), "FHIR .json files expected.");
        OptionSpecBuilder measureNameBuilder = parser.acceptsAll(asList(MEASURE_NAME_OPTIONS), "Required if measure-path specified. Mutually exclusive with library-name.");
        OptionSpecBuilder measureVersionBuilder = parser.acceptsAll(asList(LIBRARY_VERSION_OPTIONS), "If omitted most recent version of the measure will be used");  
        
        // Set up inter-depedencies.
        // Can't define libraries inline and in a directory
        parser.mutuallyExclusive(libraryBuilder, libraryPathBuilder);
        
         // Can't define expressions and a library name
        parser.mutuallyExclusive(expressionBuilder, libraryNameBuilder);
        
         // Can't define libraries and measures
        parser.mutuallyExclusive(measureNameBuilder, libraryNameBuilder);
        parser.mutuallyExclusive(measureNameBuilder, libraryBuilder);
        parser.mutuallyExclusive(measurePathBuilder, libraryNameBuilder);
        parser.mutuallyExclusive(measurePathBuilder, libraryBuilder);

        OptionSpec<String> library = libraryBuilder.withRequiredArg().describedAs("library content");
        OptionSpec<String> libraryPath = libraryPathBuilder.requiredUnless("l", "mp", "mn").withRequiredArg().describedAs("input directory for libraries");
        OptionSpec<String> libraryName = libraryNameBuilder.withRequiredArg().describedAs("name of primary library");
        OptionSpec<String> libraryVersion = libraryVersionBuilder.availableIf(libraryName).withRequiredArg().describedAs("version of primary library");
        OptionSpec<String> expression = expressionBuilder.withRequiredArg().describedAs("expression to evaluate");

        OptionSpec<String> measureName = measureNameBuilder.withRequiredArg().describedAs("name of measure");
        OptionSpec<String> measureVersion = measureVersionBuilder.availableIf(measureName).withRequiredArg().describedAs("version of measure");
        OptionSpec<String> measurePath = measurePathBuilder.requiredUnless("l", "lp", "ln").withRequiredArg().describedAs("input directory for measures");


        // TODO: Terminology user / password (and other auth options)
        OptionSpec<String> terminologyUri = parser.acceptsAll(asList(TERMINOLOGY_URI_OPTIONS),"Supports FHIR-based terminology")
            .withRequiredArg().describedAs("uri of terminology server");

        // TODO: Data provider user/ password (and other auth options)
        OptionSpec<KeyValuePair> modelUri = parser.acceptsAll(asList(MODEL_OPTIONS), 
            "Use the form model=uri. (e.g. FHIR=path/to/fhir/resources) Use multiple times to specify multiple models.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("path of data for model ");

        OptionSpec<KeyValuePair> parameter = parser.acceptsAll(asList(PARAMETER_OPTIONS), 
            "Use the form [libraryName.]parameterName=value. (e.g. Common.\"Measurement Period\"=[@2015-01-01, @2016-01-01]). Library name is optional. If omitted parameter will be set for all libraries. The parameter values must be CQL types. Use multiple times to specify multiple parameters.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("value of parameter");

        OptionSpec<KeyValuePair> context = parser.acceptsAll(asList(CONTEXT_PARAMETER_OPTIONS), 
            "Use the form contextParameterName=value. (e.g. Patient=123) Use multiple times to specify multiple context parameters.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("value of context parameter");

        OptionSpec<Boolean> verbose = parser.acceptsAll(asList(OUTPUT_FORMAT_OPTIONS), "Show simplified results")
        .withOptionalArg().ofType(Boolean.class).describedAs("String representation of a boolean value");

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

    public BuilderParameters parseAndConvert(String[] args) {
        OptionSet options = this.parse(args);

        List<String> libraries = (List<String>)options.valuesOf(LIBRARY_OPTIONS[0]);
        String libraryPath = (String)options.valueOf(LIBRARY_PATH_OPTIONS[0]);
        String libraryName = (String)options.valueOf(LIBRARY_NAME_OPTIONS[0]);
        String libraryVersion = (String)options.valueOf(LIBRARY_VERSION_OPTIONS[0]);
        List<String> expressions = (List<String>)options.valuesOf(EXPRESSION_OPTIONS[0]);

        String measurePath =  (String)options.valueOf(MEASURE_PATH_OPTIONS[0]);
        String measureName =  (String)options.valueOf(MEASURE_NAME_OPTIONS[0]);
        String measureVersion =  (String)options.valueOf(MEASURE_VERSION_OPTIONS[0]);

        // This is validation we couldn't define in terms of the jopt API.
        if ((libraries.size() > 1 || libraryPath != null)  && !(libraryName != null || !expressions.isEmpty())){
            new IllegalArgumentException("When more than one library is defined OR a library directory is selected you must define either a primary library or a set of expressions to evaluate.");
        }
    
        String terminologyUri = (String)options.valueOf(TERMINOLOGY_URI_OPTIONS[0]);
        Boolean verbose = (Boolean)options.valueOf(OUTPUT_FORMAT_OPTIONS[0]);

        List<KeyValuePair> models = (List<KeyValuePair>)options.valuesOf(MODEL_OPTIONS[0]);
        List<KeyValuePair> parameters = (List<KeyValuePair>)options.valuesOf(PARAMETER_OPTIONS[0]);
        List<KeyValuePair> contextParameters = (List<KeyValuePair>)options.valuesOf(CONTEXT_PARAMETER_OPTIONS[0]);

        BuilderParameters ip = new BuilderParameters();
        ip.libraries = libraries;
        ip.library = libraryPath;
        ip.libraryName = libraryName;
        ip.libraryVersion = libraryVersion;
        //ip.expressions = toListOfExpressions(expressions);
        ip.terminology = terminologyUri;
        ip.models = toModelInfoList(models);
        //ip.parameters = toParameterInfoList(parameters);
        ip.contextParameters = toMap("Context Parameters", contextParameters);
        ip.verbose = verbose;

        return ip;

    }

    private List<ModelInfo> toModelInfoList(List<KeyValuePair> keyValuePairs) {
        HashMap<String, String> map = new HashMap<>();
        for (KeyValuePair kvp : keyValuePairs) {
            if (map.containsKey(kvp.key)) {
                throw new IllegalArgumentException(String.format("%s contain multiple definitions for %s.", "Model parameters", kvp.key));
            }

            map.put(kvp.key, kvp.value);
        }
        return map.entrySet().stream().map(x -> new ModelInfo().withName(x.getKey()).withUrl(x.getValue())).collect(Collectors.toList());
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

    // private List<ExpressionInfo> toListOfExpressions(List<String> strings) {
    //     List<ExpressionInfo> listOfExpressions = new  ArrayList<ExpressionInfo>();

    //     for (String s : strings) {
    //         String[] parts = s.split("\\.");
    //         if (parts == null || parts.length < 2) {
    //             new IllegalArgumentException(String.format("%s is not a valid expression. Use the format libraryName.expressionName.", s));
    //         }

    //         listOfExpressions.add(new ExpressionInfo(parts[0], parts[1]));
    //     }

    //     return listOfExpressions;
    // }

    // Converts parameters from the CLI format of [libraryName.]parameterName=value to a map. Library name is optional.
    // private List<ParameterInfo> toParameterInfoList(List<KeyValuePair> keyValuePairs) {
    //     List<ParameterInfo> list = new ArrayList<>();

    //     for (KeyValuePair kvp : keyValuePairs) {
    //         String[] parts = kvp.key.split(".");
    //         list.add(new ParameterInfo(parts.length > 1 ? parts[0] : null, parts.length > 1 ? parts[1] : parts[0], kvp.value));
    //     }

    //     return list;
    // }
}