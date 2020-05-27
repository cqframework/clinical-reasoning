package org.opencds.cqf.cql.evaluator.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.UsingDef;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.Helpers;
import org.opencds.cqf.cql.evaluator.cli.temporary.EvaluationParameters;
import org.opencds.cqf.cql.evaluator.cli.temporary.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.execution.provider.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.execution.provider.NoOpRetrieveProvider;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.execution.util.DirectoryBundler;

import ca.uhn.fhir.context.FhirContext;

public class Main {

    public static void main(String[] args) {
        EvaluationParameters parameters = null;
        try {
            parameters = new ArgumentProcessor().parseAndConvert(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            // This is all temporary garbage to get running again.
            Objects.requireNonNull(parameters.contextParameter, "Gotta have a contextParameter.");
            Objects.requireNonNull(parameters.libraryName, "Gotta have a libraryName");
            Objects.requireNonNull(parameters.libraryUrl, "Gotta have a libraryUrl");
            Objects.requireNonNull(parameters.terminologyUrl, "Gotta have a terminologyUrl");
            Objects.requireNonNull(parameters.model, "Gotta have a model");
            if (!Helpers.isFileUri(parameters.libraryUrl)) {
                throw new IllegalArgumentException("libraryUrl must be a local directory for now. Sorry!");
            }

            if (!Helpers.isFileUri(parameters.terminologyUrl)) {
                throw new IllegalArgumentException("terminologyUrl must be a local directory for now. Sorry!");
            }

            if (!Helpers.isFileUri(parameters.model.getValue())) {
                throw new IllegalArgumentException("model Urls must be a local directory for now. Sorry!");
            }

            LibraryLoader libraryLoader = new LibraryLoaderFactory().create(parameters.libraryUrl);

            Map<VersionedIdentifier, Library> libraries = new HashMap<VersionedIdentifier, Library>();
            if (parameters.libraryName != null) {
                Library lib = libraryLoader.load(toExecutionIdentifier(parameters.libraryName, null));
                if (lib != null) {
                    libraries.put(lib.getIdentifier(), lib);
                }
            }

            Map<String, Pair<String, String>> modelVersionAndUrls = getModelVersionAndUrls(libraries, parameters.model);
            TerminologyProvider terminologyProvider = create(modelVersionAndUrls, parameters.terminologyUrl);
            Map<String, DataProvider> dataProviders = create(modelVersionAndUrls, terminologyProvider);

            CqlEvaluator evaluator = new CqlEvaluator(libraryLoader, parameters.libraryName, dataProviders,
                    terminologyProvider);
            var contextParameter = evaluator.unmarshalContextParameter(parameters.contextParameter);
            EvaluationResult result = evaluator.evaluate(contextParameter);

            for (var libraryEntry : result.expressionResults.entrySet()) {
                System.out.println(libraryEntry.getKey() + "=" + libraryEntry.getValue().toString());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // TODO: Remove this once builder is complete:
    private static TerminologyProvider create(Map<String, Pair<String, String>> modelVersionsAndUrls,
            String terminologyUri) {
        if (terminologyUri == null || terminologyUri.isEmpty()) {
            return null;
        }

        // We currently only support FHIR-based terminology
        // We assume that the terminology version is the same
        // As the data version
        Pair<String, String> versionAndUrl = modelVersionsAndUrls.get("http://hl7.org/fhir");
        if (versionAndUrl == null) {
            // Assume FHIR 3.0.0
            versionAndUrl = Pair.of("3.0.0", null);
        }

        FhirContext fhirContext;
        var version = versionAndUrl.getLeft();
        if (version.startsWith("5")) {
            throw new IllegalArgumentException("FHIR R5 not yet supported");
        } else if (version.startsWith("4")) {
            fhirContext = FhirContext.forR4();
        } else if (version.startsWith("3")) {
            fhirContext = FhirContext.forDstu3();
        } else {
            throw new IllegalArgumentException("FHIR DSTU2 and below not supported");
        }

        return new BundleTerminologyProvider(fhirContext, new DirectoryBundler(fhirContext).bundle(terminologyUri));
    }

    // TODO: More stuff to remove once builder is ready.
    private static Map<String, Pair<String, String>> getModelVersionAndUrls(Map<VersionedIdentifier, Library> libraries,
            Pair<String, String> modelUrl) {

        modelUrl = expandAliasToUri(modelUrl);
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
                    } else if (uri.equals(modelUrl.getLeft())) {
                        versions.put(uri, Pair.of(version, modelUrl.getRight()));
                    }
                    else {
                        versions.put(uri, Pair.of(version, null));
                    }
                }
            }
        }

        return versions;
    }

    private static Pair<String, String> expandAliasToUri(Pair<String, String> modelUrl) {
        final Map<String, String> aliasMap = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                put("FHIR", "http://hl7.org/fhir");
                put("QUICK", "http://hl7.org/fhir");
                put("QDM", "urn:healthit-gov:qdm:v5_4");
            }
        };

        if (modelUrl == null) {
            return null;
        }

        if (aliasMap.containsKey(modelUrl.getLeft())) {
            return Pair.of(aliasMap.get(modelUrl.getLeft()), modelUrl.getRight());
        }

        return modelUrl;
    }

    public static VersionedIdentifier toExecutionIdentifier(String name, String version) {
        return new VersionedIdentifier().withId(name).withVersion(version);
    }

    public static Map<String, DataProvider> create(Map<String, Pair<String, String>> modelVersionsAndUrls,
            TerminologyProvider terminologyProvider) {
        return getProviders(modelVersionsAndUrls, terminologyProvider);
    }

    private static Map<String, DataProvider> getProviders(Map<String, Pair<String, String>> versions,
            TerminologyProvider terminologyProvider) {
        Map<String, DataProvider> providers = new HashMap<>();
        for (Map.Entry<String, Pair<String, String>> m : versions.entrySet()) {
            providers.put(m.getKey(),
                    getProvider(m.getKey(), m.getValue().getLeft(), m.getValue().getRight(), terminologyProvider));
        }

        return providers;
    }

    private static DataProvider getProvider(String model, String version, String url,
            TerminologyProvider terminologyProvider) {
        switch (model) {
            case "http://hl7.org/fhir":
                return getFhirProvider(version, url, terminologyProvider);

            case "urn:healthit-gov:qdm:v5_4":
                return getQdmProvider(version, url, terminologyProvider);

            default:
                throw new IllegalArgumentException(String.format("Unknown data provider uri: %s", model));
        }
    }

    @SuppressWarnings("rawtypes")
    private static DataProvider getFhirProvider(String version, String url, TerminologyProvider terminologyProvider) {
        FhirModelResolver modelResolver;
        RetrieveProvider retrieveProvider;
        if (version.startsWith("5")) {
            throw new IllegalArgumentException("FHIR R5 not yet supported");
        } else if (version.startsWith("4")) {
            modelResolver = new R4FhirModelResolver();
        } else if (version.startsWith("3")) {
            modelResolver = new Dstu3FhirModelResolver();
        } else {
            throw new IllegalArgumentException("FHIR DSTU2 and below not supported");
        }

        if (url == null) {
            retrieveProvider = new NoOpRetrieveProvider();
        } else {
            retrieveProvider = new BundleRetrieveProvider(modelResolver, new DirectoryBundler(modelResolver.getFhirContext()).bundle(url), terminologyProvider);
        }

        return new CompositeDataProvider(modelResolver, retrieveProvider);
    }

    private static DataProvider getQdmProvider(String version, String uri, TerminologyProvider terminologyProvider) {
        throw new NotImplementedException("QDM data providers are not yet implemented");
    }

}
