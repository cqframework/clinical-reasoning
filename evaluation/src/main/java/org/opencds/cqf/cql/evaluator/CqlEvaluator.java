package org.opencds.cqf.cql.evaluator;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.cqframework.cql.elm.execution.IncludeDef;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.evaluator.resolver.ParameterResolver;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.CqlEngine.Options;
import org.opencds.cqf.cql.execution.EvaluationResult;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.execution.LibraryResult;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

// TODO: Add debug options
public class CqlEvaluator {

    private LibraryLoader libraryLoader; 
    private Map<String, DataProvider> dataProviders;
    private TerminologyProvider terminologyProvider;
    private EnumSet<Options> engineOptions;

    private ParameterResolver parameterResolver;
    private VersionedIdentifier primaryLibraryIdentifier;

    private Library primaryLibrary;
    private Set<String> defaultExpressions;

    private Map<VersionedIdentifier, Library> libraryCache;

    public CqlEvaluator(LibraryLoader libraryLoader, String primaryLibrary) {
        this(libraryLoader, primaryLibrary, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier primaryLibrary) {
        this(libraryLoader, primaryLibrary, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String primaryLibrary, TerminologyProvider terminologyProvider, Map<String, DataProvider> dataProviders) {
        this(libraryLoader, primaryLibrary, terminologyProvider, dataProviders, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier primaryLibrary, TerminologyProvider terminologyProvider, Map<String, DataProvider> dataProviders) {
        this(libraryLoader, primaryLibrary, terminologyProvider, dataProviders, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String primaryLibrary, EnumSet<Options> engineOptions) {
        this(libraryLoader, primaryLibrary, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier primaryLibrary, EnumSet<Options> engineOptions) {
        this(libraryLoader, primaryLibrary, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String primaryLibrary, TerminologyProvider terminologyProvider, Map<String, DataProvider> dataProviders, EnumSet<Options> engineOptions, ParameterResolver parameterResolver) {
        this(libraryLoader, new VersionedIdentifier().withId(primaryLibrary), terminologyProvider, dataProviders, engineOptions, parameterResolver);
    }   


    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier primaryLibraryIdentifier, TerminologyProvider terminologyProvider, Map<String, DataProvider> dataProviders, EnumSet<Options> engineOptions, ParameterResolver parameterResolver) {

        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader can not be null.");
        }

        if (primaryLibraryIdentifier == null) {
            throw new IllegalArgumentException("primaryLibrary can not be null.");
        }

        if (engineOptions == null) {
            engineOptions = EnumSet.of(org.opencds.cqf.cql.execution.CqlEngine.Options.EnableExpressionCaching);
        }
    
        this.libraryLoader = libraryLoader;
        this.dataProviders = dataProviders;
        this.terminologyProvider = terminologyProvider;
        this.engineOptions = engineOptions;

        this.parameterResolver = parameterResolver;
        this.primaryLibraryIdentifier = primaryLibraryIdentifier;

        // TODO: This needs to be moved back up the chain into the engine.
        // The engine API isn't quite right after some experimentation.

        this.primaryLibrary = this.loadAndValidate(this.libraryCache, this.primaryLibraryIdentifier);
        this.defaultExpressions = this.getExpressionSet(this.primaryLibrary);
    }

    public EvaluationResult evaluate() {
        return this.evaluate(this.defaultExpressions, null, null);
    }

    public EvaluationResult evaluate(Set<String> expressions) {
        return this.evaluate(expressions, null, null);
    }

    public EvaluationResult evaluate(Set<String> expressions, Pair<String, Object> contextParameter) {
        return this.evaluate(expressions, contextParameter, null);
    }

    public EvaluationResult evaluate(Set<String> expressions, Map<String, Object> parameters) {
        return this.evaluate(expressions, null, parameters);
    }

    public EvaluationResult evaluate(Pair<String, Object> contextParameter) {
        return this.evaluate(null, contextParameter, null);
    }

    public EvaluationResult evaluate(Pair<String, Object> contextParameter, Map<String, Object> parameters) {
        return this.evaluate(null, contextParameter, parameters);
    }

    public EvaluationResult evaluate(Map<String, Object> parameters) {
        return this.evaluate(null, null, parameters);
    }

    public EvaluationResult evaluate(Set<String> expressions, Pair<String, Object> contextParameter, Map<String, Object> parameters) {
        if (expressions == null) {
            expressions = this.defaultExpressions;
        }

        // TODO: Some testing to see if it's more performant to reset a context rather than create a new one.
        Context context = this.initializeContext(this.primaryLibrary);
        this.setParametersForContext(context, contextParameter, parameters);

        // TODO: This all needs a re-work too.
        EvaluationResult result = new EvaluationResult();
        LibraryResult libraryResult = this.evaluateLibrary(context, this.primaryLibrary, expressions);

        result.libraryResults.put(this.primaryLibraryIdentifier, libraryResult);

        return result;
    }

    public Pair<String, Object> resolveContextParameter(Pair<String, String> unresolvedContextParameter) {
        // TODO: Parse and validate context parameter (if needed)
        //this.parameterResolver.resolveContextParameters(parameters)
        return Pair.of(unresolvedContextParameter.getLeft(), unresolvedContextParameter.getRight());
    }

    public Map<String, Object> resolveParameters(Map<String, String> unresolvedParameters) {
        // TODO: Parse and validate parameter
       // return this.parameterResolver.resolveParameters(unresolvedParameters);

       Map<String, Object> resolved = new HashMap<>();
       for (Map.Entry<String, String> entry : unresolvedParameters.entrySet()) {
           resolved.put(entry.getKey(), entry.getValue());
       }

       return resolved;
    }

    private LibraryResult evaluateLibrary(Context context, Library library, Set<String> expressions) {
        LibraryResult result = new LibraryResult();

        for (String expression : expressions) {
            ExpressionDef def = context.resolveExpressionRef(expression);

            // TODO: We should probably move this validation further up the chain.
            // For example, we should tell the user that they've tried to evaluate a function def through incorrect
            // CQL or input parameters. And the code that gather the list of expressions to evaluate together should
            // not include function refs.
            if (def instanceof FunctionDef) {
                continue;
            }
            
            context.enterContext(def.getContext());
            Object object = def.evaluate(context);
            result.expressionResults.put(expression, object);
        }

        return result;
    }

    private void setParametersForContext(Context context, Pair<String, Object> contextParameter, Map<String, Object> parameters) {
        if (contextParameter != null) {
            context.setContextValue(contextParameter.getLeft(), contextParameter.getRight());
        }

        if (parameters != null) {
            for (VersionedIdentifier identifier : this.libraryCache.keySet()) {
                for (Map.Entry<String, Object> parameterValue : parameters.entrySet()) {
                    context.setParameter(identifier.getId(), parameterValue.getKey(), parameterValue.getValue());
                }
            }
        }
    }

    private Context initializeContext(Library library) {
        // Context requires an initial library to init properly.
        // TODO: Allow context to be initialized with multiple libraries
        Context context = new Context(library);

        // TODO: Does the context actually need a library loaded if all the libraries are prefetched?
        // We'd have to make sure we include the dependencies too.
        context.registerLibraryLoader(this.libraryLoader);

        if (this.engineOptions.contains(Options.EnableExpressionCaching)) {
            context.setExpressionCaching(true);
        }

        if (this.terminologyProvider != null) {
            context.registerTerminologyProvider(this.terminologyProvider);
        }
        
        if (this.dataProviders != null) {
            for (Map.Entry<String, DataProvider> pair : this.dataProviders.entrySet()) {
                context.registerDataProvider(pair.getKey(), pair.getValue());
            }
        }

        return context;
    }

    private Library loadAndValidate(Map<VersionedIdentifier, Library> libraryCache, VersionedIdentifier libraryIdentifier) {
        Library library = this.libraryLoader.load(libraryIdentifier);

        if (library == null) {
            throw new IllegalArgumentException(String.format("Unable to load library %s", 
                libraryIdentifier.getId() + libraryIdentifier.getVersion() != null ? "-" + libraryIdentifier.getVersion() : ""));
        }

        // TODO: Removed this validation pending more intelligent handling at the service layer
        // For example, providing a mock or dummy data provider in the event there's no data store
        this.validateDataRequirements(library);
        this.validateTerminologyRequirements(library);


        // TODO: Optimization ?
        // TODO: Validate Expressions as well?

        if (library.getIncludes() != null && library.getIncludes().getDef() != null) {
            for (IncludeDef include : library.getIncludes().getDef()) {
                this.loadAndValidate(libraryCache, new VersionedIdentifier().withId(include.getLocalId()).withVersion(include.getVersion()));
            }
        }

        libraryCache.put(libraryIdentifier, library);
        return library;
    }

    private void validateDataRequirements(Library library) {
        // TODO: What we actually need here is a check of the actual retrieves.

        // if (library.getUsings() != null && library.getUsings().getDef() != null && !library.getUsings().getDef().isEmpty())
        // {
        //     for (UsingDef using : library.getUsings().getDef()) {
        //         // Skip system using since the context automatically registers that.
        //         if (using.getUri().equals("urn:hl7-org:elm-types:r1"))
        //         {
        //             continue;
        //         }

        //         if (this.dataProviders == null || !this.dataProviders.containsKey(using.getUri())) {
        //             throw new IllegalArgumentException(String.format("Library %1$s is using %2$s and no data provider is registered for uri %2$s.",
        //             this.getLibraryDescription(library.getIdentifier()),
        //             using.getUri()));
        //         }
        //     }
        // }
    }

    private void validateTerminologyRequirements(Library library) {
        if ((library.getCodeSystems() != null && library.getCodeSystems().getDef() != null && !library.getCodeSystems().getDef().isEmpty()) || 
            (library.getCodes() != null  && library.getCodes().getDef() != null && !library.getCodes().getDef().isEmpty()) || 
            (library.getValueSets() != null  && library.getValueSets().getDef() != null && !library.getValueSets().getDef().isEmpty())) {
            if (this.terminologyProvider == null) {
                throw new IllegalArgumentException(String.format("Library %s has terminology requirements and no terminology provider is registered.",
                    this.getLibraryDescription(library.getIdentifier())));
            }
        }
    }

    private String getLibraryDescription(VersionedIdentifier libraryIdentifier) {
        return libraryIdentifier.getId() + (libraryIdentifier.getVersion() != null ? ("-" + libraryIdentifier.getVersion()) : "");
    }

    private Set<String> getExpressionSet(Library library) {
        Set<String> expressionNames = new HashSet<>();
        if (library.getStatements() != null && library.getStatements().getDef() != null) {
            for (ExpressionDef ed : library.getStatements().getDef()) {
                expressionNames.add(ed.getName());
            }
        }

        return expressionNames;
    }

}