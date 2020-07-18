package org.opencds.cqf.cql.evaluator.cli;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import joptsimple.util.KeyValuePair;

import org.opencds.cqf.cql.evaluator.cli.temporary.EvaluationParameters;

@SuppressWarnings({ "unchecked", "unused"})
public class ArgumentProcessor {
    public static final String[] LIBRARY_OPTIONS = {"l", "library"};

    public static final String[] LIBRARY_URL_OPTIONS = {"lu", "library-url"};
    public static final String[] LIBRARY_NAME_OPTIONS = {"ln", "library-name"};
    public static final String[] LIBRARY_VERSION_OPTIONS = {"lv", "library-version"};
    public static final String[] EXPRESSION_NAME_OPTIONS = {"en", "expression-name"};

    public static final String[] MEASURE_URL_OPTIONS = { "mu", "measure-url"};
    public static final String[] MEASURE_NAME_OPTIONS = {"mn", "measure-name"};
    public static final String[] MEASURE_VERSION_OPTIONS = {"mv", "measure-version"};

    public static final String[] TERMINOLOGY_URL_OPTIONS = {"t","terminology", "terminology-url"};
    public static final String[] MODEL_URL_OPTIONS = {"m","model", "model-url"};
    public static final String[] PARAMETER_OPTIONS = {"p", "parameter"};
    public static final String[] CONTEXT_PARAMETER_OPTIONS = {"c", "context"};



    public static final String[] HELP_OPTIONS = {"h", "help", "?"};
    public static final String[] CLI_VERSION_OPTIONS = {"v", "version"};
    public static final String[] OUTPUT_FORMAT_OPTIONS = {"f", "format"};

    public OptionParser build() {
        OptionParser parser = new OptionParser();


        OptionSpecBuilder versionBuilder = parser.acceptsAll(asList(CLI_VERSION_OPTIONS),"Show version of cql-evaluation cli.");

        OptionSpecBuilder libraryBuilder = parser.acceptsAll(asList(LIBRARY_OPTIONS),"Use multiple times to define multiple libraries.");
        
        OptionSpecBuilder libraryUrlBuilder = parser.acceptsAll(asList(LIBRARY_URL_OPTIONS), "Can be a local directory or a FHIR server base url. If a local directory, .cql files OR FHIR .json/.xml files are expected. All libraries in the directory will be included in the evaluation context.");
        OptionSpecBuilder libraryNameBuilder = parser.acceptsAll(asList(LIBRARY_NAME_OPTIONS), "Required if multiple libraries are defined inline OR a library-url is specified."); 
        OptionSpecBuilder libraryVersionBuilder = parser.acceptsAll(asList(LIBRARY_VERSION_OPTIONS), "If omitted most recent version of the library will be used.");     
        OptionSpecBuilder expressionNameBuilder = parser.acceptsAll(asList(EXPRESSION_NAME_OPTIONS), "Use the form expressionName (e.g. \"Numerator\"). Use multiple times to specify multiple expressions. If omitted all the expressions in the primary library will be evaluated.");

        OptionSpecBuilder measureUrlBuilder = parser.acceptsAll(asList(MEASURE_URL_OPTIONS), "Can be a local directory or a FHIR server base url. If a local directory FHIR .json/.xml files are expected. Mutually exclusive with library and library-url.");
        OptionSpecBuilder measureNameBuilder = parser.acceptsAll(asList(MEASURE_NAME_OPTIONS), "Required if measure-url specified.");
        OptionSpecBuilder measureVersionBuilder = parser.acceptsAll(asList(MEASURE_VERSION_OPTIONS), "If omitted most recent version of the measure will be used.");  
        
        // Set up inter-dependencies.
        // Can't define libraries inline and in a directory
        parser.mutuallyExclusive(libraryBuilder, libraryUrlBuilder);
        
         // Can't define libraries and measures
        parser.mutuallyExclusive(measureNameBuilder, libraryNameBuilder);
        parser.mutuallyExclusive(measureNameBuilder, libraryBuilder);
        parser.mutuallyExclusive(measureUrlBuilder, libraryNameBuilder);
        parser.mutuallyExclusive(measureUrlBuilder, libraryBuilder);

        OptionSpec<Void> version = versionBuilder.availableUnless("l", "mu", "mn", "lu", "ln");

        OptionSpec<String> library = libraryBuilder.withRequiredArg().describedAs("library content");
        OptionSpec<String> libraryUrl = libraryUrlBuilder.requiredUnless("l", "mu", "mn", "v").withRequiredArg().describedAs("location of libraries");
        OptionSpec<String> libraryName = libraryNameBuilder.withRequiredArg().describedAs("name of primary library");
        OptionSpec<String> libraryVersion = libraryVersionBuilder.availableIf(libraryName).withRequiredArg().describedAs("version of primary library");
        OptionSpec<String> expressionName = expressionNameBuilder.withRequiredArg().describedAs("expression name in primary library to evaluate");

        OptionSpec<String> measureName = measureNameBuilder.withRequiredArg().describedAs("name of measure");
        OptionSpec<String> measureVersion = measureVersionBuilder.availableIf(measureName).withRequiredArg().describedAs("version of measure");
        OptionSpec<String> measureUrl = measureUrlBuilder.requiredUnless("l", "lu", "ln", "v").withRequiredArg().describedAs("location of measure");


        // TODO: Terminology user / password (and other auth options)
        OptionSpec<String> terminologyUrl = parser.acceptsAll(asList(TERMINOLOGY_URL_OPTIONS),"Can be a local directory or a FHIR server url. If a local directory FHIR .json/.xml files are expected.")
            .withRequiredArg().describedAs("location of terminology");

        // TODO: Data provider user/ password (and other auth options)
        OptionSpec<KeyValuePair> modelUrl = parser.acceptsAll(asList(MODEL_URL_OPTIONS), 
            "Use the form model=url. (e.g. FHIR=path/to/fhir/resources). Can be a local directory or a FHIR server url. If a local directory FHIR .json/.xml files are expected.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("location of data for model ");

        OptionSpec<KeyValuePair> parameter = parser.acceptsAll(asList(PARAMETER_OPTIONS), 
            "Use the form parameterName=value (e.g. \"Measurement Period\"=[@2015-01-01, @2016-01-01]). The parameter values must be CQL or FHIR types. Use multiple times to specify multiple parameters.")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("value of parameter");

        OptionSpec<KeyValuePair> context = parser.acceptsAll(asList(CONTEXT_PARAMETER_OPTIONS), 
            "Use the form contextParameter=value (e.g. Patient=123).")
            .withRequiredArg().ofType(KeyValuePair.class).describedAs("name and value of context parameter");

        // OptionSpec<Boolean> verbose = parser.acceptsAll(asList(OUTPUT_FORMAT_OPTIONS), "Show simplified results")
        // .withOptionalArg().ofType(Boolean.class).describedAs("String representation of a boolean value");

        OptionSpec<Void> help = parser.acceptsAll(asList(HELP_OPTIONS), "Show this help page").forHelp();

        return parser;
    }

    public OptionSet parse(String[] args) {
        OptionParser parser = build();
        OptionSet options = parser.parse(args);
        if (options.has(HELP_OPTIONS[0])) {
            try {
                this.printVersion();
                parser.printHelpOn(System.out);
                this.printExamples();
            }
            catch (Exception e) {

            }

            System.exit(0);
        }

        if (options.has(CLI_VERSION_OPTIONS[0])) {
            try {
                this.printVersion();
            }
            catch (Exception e) {

            }

            System.exit(0);
        }

        return options;
    }

    private void printVersion() {
        System.out.println(String.format("cql-evaluator cli version: %s", ArgumentProcessor.class.getPackage().getImplementationVersion()));
    }

    private void printExamples() {
        System.out.println("Examples:");
        System.out.println("cli --ln \"CMS146\" --lu /ig/cql --m FHIR=/ig/tests --t /ig/valuesets -c Patient=123");
    }

    public EvaluationParameters parseAndConvert(String[] args) {
        OptionSet options = this.parse(args);

        List<String> libraries = (List<String>)options.valuesOf(LIBRARY_OPTIONS[0]);
        String libraryUrl = (String)options.valueOf(LIBRARY_URL_OPTIONS[0]);
        String libraryName = (String)options.valueOf(LIBRARY_NAME_OPTIONS[0]);
        String libraryVersion = (String)options.valueOf(LIBRARY_VERSION_OPTIONS[0]);
        List<String> expressions = (List<String>)options.valuesOf(EXPRESSION_NAME_OPTIONS[0]);

        String measureUrl =  (String)options.valueOf(MEASURE_URL_OPTIONS[0]);
        String measureName =  (String)options.valueOf(MEASURE_NAME_OPTIONS[0]);
        String measureVersion =  (String)options.valueOf(MEASURE_VERSION_OPTIONS[0]);
    
        String terminologyUrl = (String)options.valueOf(TERMINOLOGY_URL_OPTIONS[0]);

        // TODO: Format this to the correct list.
        //Boolean verbose = (Boolean)options.valueOf(OUTPUT_FORMAT_OPTIONS[0]);

        KeyValuePair model = (KeyValuePair)options.valueOf(MODEL_URL_OPTIONS[0]);
        List<KeyValuePair> parameters = (List<KeyValuePair>)options.valuesOf(PARAMETER_OPTIONS[0]);
        KeyValuePair contextParameter = (KeyValuePair)options.valueOf(CONTEXT_PARAMETER_OPTIONS[0]);

        EvaluationParameters ep = new EvaluationParameters();
        ep.libraryUrl = libraryUrl;
        ep.libraryName = libraryName;
        ep.contextParameter = contextParameter != null ? Pair.of(contextParameter.key, contextParameter.value) : null;
        ep.model = model != null ? Pair.of(model.key, model.value) : null;
        ep.terminologyUrl = terminologyUrl;
        return ep;
    }
}