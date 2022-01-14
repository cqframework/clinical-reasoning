package org.opencds.cqf.cql.evaluator.builder;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.EmbeddedFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatorOptionAwareLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.retrieve.NoOpRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.PriorityTerminologyProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.PrivateCachingTerminologyProviderDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to construct a CqlEvaluator that may have multiple
 * content, data, and terminology sources NOTE: The CqlEngine and
 * RetrieveProviders use terminology at two separate levels. The CqlEngine uses
 * a TerminologyProvider directly to evaluate CQL that deals with Terminology,
 * and RetrieveProviders can also use a TerminologyProvider to support filtering
 * during retrieves. The configuration of RetrieveProviders is done via the
 * RetrieveProviderConfigurer class.
 */
@Named
public class CqlEvaluatorBuilder {

    private static Logger logger = LoggerFactory.getLogger(CqlEvaluatorBuilder.class);

    private static Map<VersionedIdentifier, Model> globalModelCache = new HashMap<>();

    private List<LibraryContentProvider> libraryContentProviders;

    private List<TerminologyProvider> terminologyProviders;

    private Map<String, Pair<ModelResolver, List<RetrieveProvider>>> dataProviderParts;

    private EnumSet<CqlEngine.Options> engineOptions;

    private CqlTranslatorOptions cqlTranslatorOptions;

    private Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache;

    private Boolean useEmbeddedLibraries = true;

    private RetrieveProviderConfig retrieveProviderConfig;

    private Boolean stale = false;

    /**
     * Constructor for the CqlEvaluator builder
     */
    @Inject
    public CqlEvaluatorBuilder() {
        this.libraryContentProviders = new ArrayList<>();
        this.terminologyProviders = new ArrayList<>();
        this.dataProviderParts = new HashMap<>();
        this.libraryCache = new HashMap<>();
        this.retrieveProviderConfig = RetrieveProviderConfig.defaultConfig();
        this.engineOptions = EnumSet.of(CqlEngine.Options.EnableExpressionCaching);
    }

    /**
     * Adds a LibraryContentProvider to the list of LibraryContentProviders that may
     * be used during evaluation. This function uses FILO semantics. The first
     * LibraryContentProvider added is the last to be searched for a Library.
     * 
     * @param libraryContentProvider the libraryContentProvider to add to the
     *                               evaluation context
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withLibraryContentProvider(LibraryContentProvider libraryContentProvider) {
        requireNonNull(libraryContentProvider, "libraryLoader can not be null");

        this.libraryContentProviders.add(libraryContentProvider);
        return this;
    }

    /**
     * Adds a TerminologyProvider to the list of TerminologyProviders that may be
     * used during evaluation. This function uses FILO semantics. The first
     * TerminologyProvider added is the last to be used for Terminology.
     * 
     * @param terminologyProvider the TerminologyProvider to add to the evaluation
     *                            context
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withTerminologyProvider(TerminologyProvider terminologyProvider) {
        requireNonNull(terminologyProvider, "terminologyProvider can not be null");

        this.terminologyProviders.add(terminologyProvider);
        return this;
    }

    /**
     * Adds a ModelResolver for a given model to the evaluation context. There may
     * only be one ModelResolver for a given model.
     * 
     * @param model         the modelUri
     * @param modelResolver the resolver to use
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withModelResolver(String model, ModelResolver modelResolver) {
        requireNonNull(model, "model can not be null");
        requireNonNull(modelResolver, "modelResolver can not be null");

        if (!this.dataProviderParts.containsKey(model)) {
            this.dataProviderParts.put(model, Pair.of(modelResolver, new ArrayList<>()));
        } else if (this.dataProviderParts.get(model).getLeft() == null) {
            this.dataProviderParts.put(model, Pair.of(modelResolver, this.dataProviderParts.get(model).getRight()));
        } else if (this.dataProviderParts.get(model).getLeft().equals(modelResolver)) {
            logger.debug("modelResolver is the same as pre-existing one. Ignoring.");
        } else {
            throw new IllegalArgumentException(String.format("ModelResolver already specified for model %s", model));
        }

        return this;
    }

    /**
     * Adds a RetrieveProvider for a given model to the evaluation context. This
     * function uses FILO semantics. The first RetrieveProvider added is the last to
     * be used for retrieves.
     * 
     * @param model            the modelUri
     * @param retrieveProvider the provider to use
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withRetrieveProvider(String model, RetrieveProvider retrieveProvider) {
        requireNonNull(model, "model can not be null");
        requireNonNull(retrieveProvider, "retrieveProvider can not be null");

        if (!this.dataProviderParts.containsKey(model)) {
            ArrayList<RetrieveProvider> retrieveProviders = new ArrayList<>();
            retrieveProviders.add(retrieveProvider);
            this.dataProviderParts.put(model, Pair.of(null, retrieveProviders));
        } else {
            this.dataProviderParts.get(model).getRight().add(retrieveProvider);
        }

        return this;
    }

    /**
     * Adds a ModelResolver and RetrieveProvider for a given model to the evaluation
     * context. There may only be one ModelResolver for a given model. This function
     * uses FILO semantics. The first RetrieveProvider added is the last to be used
     * for retrieves.
     * 
     * @param model            the modelUri
     * @param modelResolver    the resolver to use
     * @param retrieveProvider the provider to use
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withModelResolverAndRetrieveProvider(String model, ModelResolver modelResolver,
            RetrieveProvider retrieveProvider) {
        requireNonNull(model, "model can not be null");
        requireNonNull(modelResolver, "modelResolver can not be null");
        requireNonNull(retrieveProvider, "retrieveProvider can not be null");

        this.withModelResolver(model, modelResolver);
        this.withRetrieveProvider(model, retrieveProvider);
        return this;
    }

    /**
     * Adds a ModelResolver and RetrieveProvider for a given model to the evaluation
     * context. There may only be one ModelResolver for a given model. This function
     * uses FILO semantics. The first RetrieveProvider added is the last to be used
     * for retrieves.
     * 
     * @param modelTriple the model with a uri, modelResolver, and RetrieveProvider
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withModelResolverAndRetrieveProvider(
            Triple<String, ModelResolver, RetrieveProvider> modelTriple) {
        requireNonNull(modelTriple, "modelTriple can not be null");
        this.withModelResolverAndRetrieveProvider(modelTriple.getLeft(), modelTriple.getMiddle(),
                modelTriple.getRight());
        return this;
    }

    /**
     * Sets whether or not the CqlTranslator should use the embedded libraries that
     * are included in the translator package. If useEmbeddedLibraries is set to
     * true the embedded libraries are used as a content source, but are always
     * considered last
     * 
     * @param useEmbeddedLibraries whether to include the embedded libraries
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withUseEmbeddedLibraries(Boolean useEmbeddedLibraries) {
        this.useEmbeddedLibraries = useEmbeddedLibraries;
        return this;
    }

    /**
     * Sets the CqlTranslatorOptions to use for Cql translation when loading content
     * 
     * @param cqlTranslatorOptions the translator options to use
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withCqlTranslatorOptions(CqlTranslatorOptions cqlTranslatorOptions) {
        this.cqlTranslatorOptions = cqlTranslatorOptions;
        return this;
    }

    /**
     * Set the Library cache to use when loading CQL libraries. A cached library
     * will be verified to make sure versions match and that it was translated with
     * the same CQL options.
     * 
     * @param libraryCache the library cache
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withLibraryCache(
            HashMap<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> libraryCache) {
        this.libraryCache = libraryCache;
        return this;
    }

    /**
     * Specifies the configuration to use for the RetrieveProviders. This will be
     * applied to all registered RetrieveProviders. If you want to configure each
     * individually do so directly on the RetrieveProvider prior to registering it
     * with the builder.
     * 
     * @param retrieveProviderConfig the retrieveProviderConfig to use
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withRetrieveProviderConfig(RetrieveProviderConfig retrieveProviderConfig) {
        this.retrieveProviderConfig = retrieveProviderConfig;
        return this;
    }

    private Map<String, DataProvider> buildDataProviders(TerminologyProvider terminologyProvider) {
        Map<String, DataProvider> dataProviders = new HashMap<>();

        for (Map.Entry<String, Pair<ModelResolver, List<RetrieveProvider>>> entry : this.dataProviderParts.entrySet()) {
            ModelResolver modelResolver = entry.getValue().getLeft();
            if (modelResolver == null) {
                throw new IllegalArgumentException(String.format(
                        "No ModelResolver specified for model %s while constructing CqlEvaluator. Supply a ModelResolver prior to calling build().",
                        entry.getKey()));
            }

            List<RetrieveProvider> providers = entry.getValue().getRight();
            Collections.reverse(providers);

            if (providers.isEmpty()) {
                providers.add(new NoOpRetrieveProvider());
            }

            RetrieveProviderConfigurer retrieveProviderConfigurer = new RetrieveProviderConfigurer(
                    retrieveProviderConfig);
            for (RetrieveProvider provider : providers) {
                retrieveProviderConfigurer.configure(provider, terminologyProvider);
            }

            dataProviders.put(entry.getKey(),
                    this.decorate(new CompositeDataProvider(modelResolver, new PriorityRetrieveProvider(providers))));
        }

        return dataProviders;
    }

    private CqlTranslatorOptions getDefaultOptions() {
        CqlTranslatorOptions options = CqlTranslatorOptions.defaultOptions();
        if (!options.getFormats().contains(CqlTranslator.Format.XML)) {
            options.getFormats().add(CqlTranslator.Format.XML);
        }
        logger.info("cql-options not found. Using default options.");
        return options;
    }

    private LibraryLoader buildLibraryLoader() {
        Collections.reverse(this.libraryContentProviders);
        if (this.useEmbeddedLibraries) {
            this.libraryContentProviders.add(new EmbeddedFhirLibraryContentProvider());
        }

        if (this.cqlTranslatorOptions == null) {
            this.cqlTranslatorOptions = getDefaultOptions();
        }

        TranslatorOptionAwareLibraryLoader libraryLoader = new TranslatingLibraryLoader(
                new CacheAwareModelManager(globalModelCache), libraryContentProviders, this.cqlTranslatorOptions);
        if (this.libraryCache != null) {
            libraryLoader = new CacheAwareLibraryLoaderDecorator(libraryLoader, this.libraryCache);
        }

        return this.decorate(libraryLoader);
    }

    private TerminologyProvider buildTerminologyProvider() {
        TerminologyProvider terminologyProvider = null;
        if (terminologyProviders.size() > 1) {
            Collections.reverse(this.terminologyProviders);
            terminologyProvider = new PriorityTerminologyProvider(terminologyProviders);
        } else if (terminologyProviders.size() == 1) {
            terminologyProvider = this.terminologyProviders.get(0);
        }

        if (terminologyProvider != null) {
            terminologyProvider = this.decorate(terminologyProvider);
        }

        return terminologyProvider;
    }

    protected DataProvider decorate(DataProvider dataProvider) {
        return dataProvider;
    }

    protected TerminologyProvider decorate(TerminologyProvider terminologyProvider) {
        return new PrivateCachingTerminologyProviderDecorator(terminologyProvider);
    }

    protected LibraryLoader decorate(LibraryLoader libraryLoader) {
        return libraryLoader;
    }

    /**
     * Builds a CqlEvaluator that uses all content, data, terminology sources
     * supplied, and has the appropriate configuration applied.
     * 
     * NOTE: The CqlEvaluator created by this default implementation is meant to be
     * short-lived (e.g. for the duration of a request). It won't pick up changes to
     * underlying content.
     * 
     * @return a CqlEvaluator
     */
    public CqlEvaluator build() {
        if (this.stale) {
            throw new IllegalStateException(
                    "This instance of the CqlEvaluatorBuilder has already been used. Please instantiate a new instance to create another CqlEvaluator.");
        }

        this.stale = true;

        LibraryLoader libraryLoader = this.buildLibraryLoader();
        TerminologyProvider terminologyProvider = this.buildTerminologyProvider();
        Map<String, DataProvider> dataProviders = this.buildDataProviders(terminologyProvider);

        return new CqlEvaluator(libraryLoader, dataProviders, terminologyProvider, this.engineOptions);
    }

}
