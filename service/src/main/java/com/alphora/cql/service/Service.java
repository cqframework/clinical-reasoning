package com.alphora.cql.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alphora.cql.service.factory.DataProviderFactory;
import com.alphora.cql.service.factory.DefaultDataProviderFactory;
import com.alphora.cql.service.factory.DefaultLibraryLoaderFactory;
import com.alphora.cql.service.factory.DefaultTerminologyProviderFactory;
import com.alphora.cql.service.factory.LibraryLoaderFactory;
import com.alphora.cql.service.factory.TerminologyProviderFactory;
import com.alphora.cql.service.resolver.DefaultParameterResolver;
import com.alphora.cql.service.resolver.ParameterResolver;
import com.serialization.EvaluationResultsSerializer;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.UsingDef;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.execution.CqlEngine;
import org.opencds.cqf.cql.execution.EvaluationResult;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

public class Service {

    public enum Options {
        EnableFileUri
    }

    private EnumSet<Options> options;
    private EnumSet<org.opencds.cqf.cql.execution.CqlEngine.Options> engineOptions;
    private EnumSet<CqlTranslator.Options> translatorOptions;
    private TerminologyProviderFactory terminologyProviderFactory;
    private DataProviderFactory dataProviderFactory;
    private LibraryLoaderFactory libraryLoaderFactory;
    private ParameterResolver parameterResolver;

    // Use this as a quick way to enable file uris.
    public Service(EnumSet<Options> options) {
        this(null, null, null, null, options, null, null);
    }

    public Service(LibraryLoaderFactory libraryLoaderFactory, DataProviderFactory dataProviderFactory,
            TerminologyProviderFactory terminologyProviderFactory, ParameterResolver parameterResolver,
            EnumSet<Options> options,
            EnumSet<org.opencds.cqf.cql.execution.CqlEngine.Options> engineOptions,
            EnumSet<CqlTranslator.Options> translatorOptions) {

        if (libraryLoaderFactory == null) {
            libraryLoaderFactory = new DefaultLibraryLoaderFactory();
        }

        if (dataProviderFactory == null) {
            dataProviderFactory = new DefaultDataProviderFactory();
        }

        if (terminologyProviderFactory == null) {
            terminologyProviderFactory = new DefaultTerminologyProviderFactory();
        }

        if (parameterResolver == null) {
           parameterResolver = new DefaultParameterResolver();
        }

        if (engineOptions == null) {
            engineOptions = EnumSet.of(org.opencds.cqf.cql.execution.CqlEngine.Options.EnableExpressionCaching);
        }

        if (options == null) {
            options = EnumSet.noneOf(Options.class);
        }

        if (translatorOptions == null) {
            // Default for measure eval
            translatorOptions =  EnumSet.of(
                CqlTranslator.Options.EnableAnnotations,
                CqlTranslator.Options.EnableLocators,
                CqlTranslator.Options.DisableListDemotion,
                CqlTranslator.Options.DisableListPromotion,
                CqlTranslator.Options.DisableMethodInvocation);
        }

        this.libraryLoaderFactory = libraryLoaderFactory;
        this.dataProviderFactory = dataProviderFactory;
        this.terminologyProviderFactory = terminologyProviderFactory;
        this.parameterResolver = parameterResolver;
        this.options = options;
        this.engineOptions = engineOptions;
        this.translatorOptions = translatorOptions;
    }

    public Response evaluate(Parameters parameters) {
        validateParameters(parameters);

        LibraryLoader libraryLoader = null;
        if (parameters.libraryPath != null && !parameters.libraryPath.isEmpty()) {
            libraryLoader = this.libraryLoaderFactory.create(parameters.libraryPath, this.translatorOptions);
        }
        else {
            libraryLoader = this.libraryLoaderFactory.create(parameters.libraries, this.translatorOptions);
        }

        Map<VersionedIdentifier, Set<String>> expressions = this.toExpressionMap(parameters.expressions);
        Map<VersionedIdentifier, Map<String, String>> evaluationParameters = this.toParameterMap(parameters.parameters);

        // TOOD: Recursive resolve ALL libraries, not just those that are used by parameters, expressions, and library name.
        // Either that or have the library manager just give them all to us.

        Map<VersionedIdentifier, Library> libraries = new HashMap<VersionedIdentifier, Library>();
        if (parameters.libraryName != null) {
            Library lib = libraryLoader.load(toExecutionIdentifier(parameters.libraryName, parameters.libraryVersion));
            if (lib != null) {
                libraries.put(lib.getIdentifier(), lib);
            }
        }

        for (VersionedIdentifier v : expressions.keySet()) {
            Library lib = libraryLoader.load(v);
            if (lib != null && !libraries.containsKey(lib.getIdentifier())) {
                libraries.put(lib.getIdentifier(), lib);
            }
        }

        for (VersionedIdentifier v : evaluationParameters.keySet()) {
            Library lib = libraryLoader.load(v);
            if (lib != null && !libraries.containsKey(lib.getIdentifier())) {
                libraries.put(lib.getIdentifier(), lib);
            }
        }

        String model = null;
        String version = null;
        Map<String, String> modelsAndVersion = getModelAndVersionFromLibrary(parameters, libraries);
        for (Map.Entry<String, String> entry : modelsAndVersion.entrySet()) {
            model = entry.getKey();
            version = entry.getValue();
        }
        TerminologyProvider terminologyProvider = this.terminologyProviderFactory.create(model, version, parameters.terminologyUri);
        Map<String, DataProvider> dataProviders = this.dataProviderFactory.create(libraries, parameters.modelUris, terminologyProvider);

        Map<String, Object> resolvedContextParameters = this.parameterResolver.resolvecontextParameters(parameters.contextParameters);
        Map<VersionedIdentifier, Map<String, Object>> resolvedEvaluationParameters = this.parameterResolver.resolveParameters(libraries, evaluationParameters);

        CqlEngine engine = new CqlEngine(libraryLoader, dataProviders, terminologyProvider, this.engineOptions);

        EvaluationResult result = null;
        if (parameters.libraryName != null) {
            result = engine.evaluate(resolvedContextParameters, resolvedEvaluationParameters,
                    this.toExecutionIdentifier(parameters.libraryName, null));
        } else {
            result = engine.evaluate(resolvedContextParameters, resolvedEvaluationParameters, expressions);
        }

        Response response = new Response();
        response.evaluationResult = result;
        EvaluationResultsSerializer.setFhirContext(version);
        return response;
    }

    private Map<String, String> getModelAndVersionFromLibrary(Parameters parameters, Map<VersionedIdentifier, Library> libraries) {
        Map<String, String> modelsAndVersion = new HashMap<String, String>();
        final Map<String, String> shorthandMap = new HashMap<String, String>() {
            {
                put("FHIR", "http://hl7.org/fhir");
                put("QUICK", "http://hl7.org/fhir");
                put("QDM", "urn:healthit-gov:qdm:v5_4");
            }
        };
        //Assuming it's the same version as the library for now.
        String model = null;
        String version = null;
        for (Map.Entry<String, String> modelUri : parameters.modelUris.entrySet()) {
            String uri = shorthandMap.containsKey(modelUri.getKey()) ? shorthandMap.get(modelUri.getKey())
                    : modelUri.getKey();
            model = modelUri.getKey();
            for (Library library : libraries.values()) {
                if (version != null) {
                    break;
                }

                if (library.getUsings() != null && library.getUsings().getDef() != null) {
                    for (UsingDef u : library.getUsings().getDef()) {
                        if (u.getUri().equals(uri)) {
                            version = u.getVersion();
                            break;
                        }
                    }
                }
            }

            if (model == null) {
                model = "FHIR";
            }
            
            if (version == null) {
                version = "3.0.0";
            }
            modelsAndVersion.put(model, version);
        }
        return modelsAndVersion;
    }

    public VersionedIdentifier toExecutionIdentifier(String name, String version) {
        return new VersionedIdentifier().withId(name).withVersion(version);
    }


    private Map<VersionedIdentifier, Set<String>> toExpressionMap(List<Pair<String, String>> expressions) {
        Map<VersionedIdentifier, Set<String>> map = new HashMap<>();
        for (Pair<String, String> p : expressions) {
            VersionedIdentifier vi = toExecutionIdentifier(p.getLeft(), null);
            if (!map.containsKey(vi)) {
                map.put(vi, new HashSet<>());
            }

            map.get(vi).add(p.getRight());
        }

        return map;
    }

    private Map<VersionedIdentifier, Map<String, String>> toParameterMap(Map<Pair<String, String>,String> parameters) {
        Map<VersionedIdentifier, Map<String, String>> map = new HashMap<>();
        for (Map.Entry<Pair<String, String>, String> p : parameters.entrySet()) {
            VersionedIdentifier vi = toExecutionIdentifier(p.getKey().getLeft(), null);
            if (!map.containsKey(vi)) {
                map.put(vi, new HashMap<>());
            }

            map.get(vi).put(p.getKey().getRight(), p.getValue());
        }

        return map;
    }

    private void ensureNotFileUri(String uri) {
        if (Helpers.isFileUri(uri)) {
            throw new IllegalArgumentException(String.format("%s is not a valid uri", uri));
        }
    }

    private void ensureNotFileUri(Collection<String> uris) {
        for (String s : uris) {
            ensureNotFileUri(s);
        }
    }

    private void validateParameters(Parameters parameters) {
          // Ensure EnableFileURI option is respected. This is a potential security risk on a public server, so this must remain implemented.
          if (!this.options.contains(Options.EnableFileUri)) {
            ensureNotFileUri(parameters.libraryPath);
            ensureNotFileUri(parameters.terminologyUri);
            ensureNotFileUri(parameters.modelUris.values());
        }

        if (parameters.libraryName == null && (parameters.expressions == null || parameters.expressions.isEmpty())) {
            throw new IllegalArgumentException("libraryName or expressions must be specified.");
        }

        if (parameters.libraryName != null && (parameters.expressions != null && !parameters.expressions.isEmpty())) {
            throw new IllegalArgumentException("libraryName and expressions are mutually exclusive. Only specify one.");
        }

        if ((parameters.libraries != null && !parameters.libraries.isEmpty()) && (parameters.libraryPath != null && !parameters.libraryPath.isEmpty())) {
            throw new IllegalArgumentException("libraries and library path are mutually exclusive. Only specify one.");
        }
    }
}