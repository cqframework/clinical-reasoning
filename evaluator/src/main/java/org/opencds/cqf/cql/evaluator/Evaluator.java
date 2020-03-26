package org.opencds.cqf.cql.evaluator;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opencds.cqf.cql.evaluator.factory.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.factory.implementation.DefaultDataProviderFactory;
import org.opencds.cqf.cql.evaluator.factory.implementation.DefaultLibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.factory.implementation.DefaultTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.factory.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.factory.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.resolver.DefaultParameterResolver;
import org.opencds.cqf.cql.evaluator.resolver.ParameterResolver;

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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class Evaluator {

    public enum Options {
        EnableFileUri
    }

    private EnumSet<Options> options;
    private EnumSet<org.opencds.cqf.cql.engine.execution.CqlEngine.Options> engineOptions;
    private EnumSet<CqlTranslator.Options> translatorOptions;
    private TerminologyProviderFactory terminologyProviderFactory;
    private DataProviderFactory dataProviderFactory;
    private LibraryLoaderFactory libraryLoaderFactory;
    private ParameterResolver parameterResolver;

    public Evaluator() {
        this(null, null, null, null, null, null, null);
    }

    // Use this as a quick way to enable file uris.
    public Evaluator(EnumSet<Options> options) {
        this(null, null, null, null, options, null, null);
    }

    public Evaluator(LibraryLoaderFactory libraryLoaderFactory, DataProviderFactory dataProviderFactory,
            TerminologyProviderFactory terminologyProviderFactory, ParameterResolver parameterResolver,
            EnumSet<Options> options, EnumSet<org.opencds.cqf.cql.engine.execution.CqlEngine.Options> engineOptions,
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
            engineOptions = EnumSet.of(org.opencds.cqf.cql.engine.execution.CqlEngine.Options.EnableExpressionCaching);
        }

        if (options == null) {
            options = EnumSet.noneOf(Options.class);
        }

        if (translatorOptions == null) {
            // Default for measure eval
            translatorOptions = EnumSet.of(CqlTranslator.Options.EnableAnnotations,
                    CqlTranslator.Options.EnableLocators, CqlTranslator.Options.DisableListDemotion,
                    CqlTranslator.Options.DisableListPromotion, CqlTranslator.Options.DisableMethodInvocation);
        }

        this.libraryLoaderFactory = libraryLoaderFactory;
        this.dataProviderFactory = dataProviderFactory;
        this.terminologyProviderFactory = terminologyProviderFactory;
        this.parameterResolver = parameterResolver;
        this.options = options;
        this.engineOptions = engineOptions;
        this.translatorOptions = translatorOptions;
    }


    // TODO: Support data and terminology being passed in directly.
    public Response evaluate(Parameters parameters) {
        validateParameters(parameters);

        LibraryLoader libraryLoader = null;
        if (parameters.libraryPath != null && !parameters.libraryPath.isEmpty()) {
            libraryLoader = this.libraryLoaderFactory.create(parameters.libraryPath, this.translatorOptions);
        } else {
            libraryLoader = this.libraryLoaderFactory.create(parameters.libraries, this.translatorOptions);
        }

        Map<VersionedIdentifier, Set<String>> expressions = this.toExpressionMap(parameters.expressions);
        Map<VersionedIdentifier, Map<String, Object>> evaluationParameters = this.toParameterMap(parameters.parameters);

        // TOOD: Recursively resolve ALL libraries, not just those that are used by parameters, expressions, and library name.
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

        parameters.models = resolveModelVersions(libraries, parameters.models);

        // HACK AROUND: The file-based fhir terminology provider doesn't yet know how to auto-detect
        // Fhir version for json or xml resources, so we have to tell it explicitly what context to you.
        FhirContext context = null;
        Optional<ModelInfo> fhirInfo = parameters.models.stream().filter(x -> x.getName().equals("http://hl7.org/fhir")).findFirst();
        if (fhirInfo.isPresent()) {
            context = FhirVersionEnum.forVersionString(fhirInfo.get().getVersion()).newContext();
        }
        TerminologyProvider terminologyProvider = this.terminologyProviderFactory.create(context, parameters.terminologyUrl, parameters.clientFactory);
        Map<String, DataProvider> dataProviders = this.dataProviderFactory.create(parameters.models, terminologyProvider, parameters.clientFactory);

        Map<String, Object> resolvedContextParameters = this.parameterResolver.resolvecontextParameters(parameters.contextParameters);
        Map<VersionedIdentifier, Map<String, Object>> resolvedEvaluationParameters = this.parameterResolver.resolveParameters(libraries, evaluationParameters);

        CqlEngine engine = new CqlEngine(libraryLoader, dataProviders, terminologyProvider, this.engineOptions);

        EvaluationResult result = null;
        if (parameters.expressions == null || parameters.expressions.isEmpty()) {
            result = engine.evaluate(parameters.libraryName, contextParams,
                    forReallyRealResolvedEvaluationParameters.isPresent()
                            ? forReallyRealResolvedEvaluationParameters.get().getValue()
                            : null);
        } else {
            result = engine.evaluate(parameters.libraryName,
                    expressions.entrySet().stream().findFirst().get().getValue(), contextParams,
                    forReallyRealResolvedEvaluationParameters.isPresent()
                            ? forReallyRealResolvedEvaluationParameters.get().getValue()
                            : null);
        }

        Response response = new Response();
        response.evaluationResult = result;

        // TODO: Non-static serializers for different models.
        // Pair<String, String> versionAndUrl = modelVersionAndUrls.get("FHIR");
        // if (versionAndUrl != null) {
        //     EvaluationResultsSerializer.setFhirContext(versionAndUrl.getLeft());
        // }

        return response;
    }

    private List<ModelInfo> resolveModelVersions(Map<VersionedIdentifier, Library> libraries,
        List<ModelInfo> models) {

        models = this.expandAliasToUri(models);
        Map<String, Pair<String, String>> versions = new HashMap<>();
        for (Library library : libraries.values()) {
            if (library.getUsings() != null && library.getUsings().getDef() != null) {
                for (UsingDef u : library.getUsings().getDef()) {
                    String uri = u.getUri();
                    // Skip the system URI
                    if (uri.equals("urn:hl7-org:elm-types:r1")) {
                        continue;
                    }
                    String version = u.getVersion();
                    if (versions.containsKey(uri)) {
                        Pair<String, String> existing = versions.get(uri);
                        if (!existing.getLeft().equals(version)) {
                            throw new IllegalArgumentException(String.format(
                                    "Libraries are using multiple versions of %s. Only one version is supported at a time.",
                                    uri));
                        }
                    }
                    else {
                        String url = models.stream().filter(x -> x.getName().equals(uri)).findFirst().get().getUrl();
                        versions.put(uri, Pair.of(version, url));
                    }
                }
            }
        }

        return versions.entrySet().stream()
            .map(x -> new ModelInfo(x.getKey(), x.getValue().getLeft(), x.getValue().getRight()))
            .collect(Collectors.toList());
    }

    private List<ModelInfo> expandAliasToUri(List<ModelInfo> models) {
        final Map<String, String> aliasMap = new HashMap<String, String>() {
            {
                put("FHIR", "http://hl7.org/fhir");
                put("QUICK", "http://hl7.org/fhir");
                put("QDM", "urn:healthit-gov:qdm:v5_4");
            }
        };

        if (models == null)
        {
            return null;
        }

        for (ModelInfo m  : models){
            if (aliasMap.containsKey(m.getName())) {
                m.setName(aliasMap.get(m.getName()));
            }
        }

        return models;
    }

    public VersionedIdentifier toExecutionIdentifier(String name, String version) {
        return new VersionedIdentifier().withId(name).withVersion(version);
    }

    private Map<VersionedIdentifier, Set<String>> toExpressionMap(List<ExpressionInfo> expressions) {
        Map<VersionedIdentifier, Set<String>> map = new HashMap<>();
        for (ExpressionInfo e : expressions) {
            VersionedIdentifier vi = toExecutionIdentifier(e.getLibraryName(), null);
            if (!map.containsKey(vi)) {
                map.put(vi, new HashSet<>());
            }

            map.get(vi).add(e.getIdentifier());
        }

        return map;
    }

    private Map<VersionedIdentifier, Map<String, Object>> toParameterMap(List<ParameterInfo> parameters) {
        Map<VersionedIdentifier, Map<String, Object>> map = new HashMap<>();
        for (ParameterInfo p : parameters) {
            VersionedIdentifier vi = toExecutionIdentifier(p.getLibraryName(), null);
            if (!map.containsKey(vi)) {
                map.put(vi, new HashMap<>());
            }

            map.get(vi).put(p.getIdentifier(), p.getValue());
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
        // Ensure EnableFileURI option is respected. This is a potential security risk
        // on a public server, so this must remain implemented.
        if (!this.options.contains(Options.EnableFileUri)) {
            ensureNotFileUri(parameters.libraryPath);
            ensureNotFileUri(parameters.terminologyUrl);
            if (parameters.models!= null) {
                ensureNotFileUri(parameters.models.stream().map(x -> x.getUrl()).collect(Collectors.toList()));
            }            
        }

        if (parameters.libraryName == null && (parameters.expressions == null || parameters.expressions.isEmpty())) {
            throw new IllegalArgumentException("libraryName or expressions must be specified.");
        }

        if (parameters.libraryName != null && (parameters.expressions != null && !parameters.expressions.isEmpty())) {
            throw new IllegalArgumentException("libraryName and expressions are mutually exclusive. Only specify one.");
        }

        if ((parameters.libraries != null && !parameters.libraries.isEmpty())
                && (parameters.libraryPath != null && !parameters.libraryPath.isEmpty())) {
            throw new IllegalArgumentException("libraries and library path are mutually exclusive. Only specify one.");
        }
    }
}