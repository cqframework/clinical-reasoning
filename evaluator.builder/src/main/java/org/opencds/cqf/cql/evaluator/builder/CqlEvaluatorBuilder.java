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
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.engine.execution.PriorityLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.PrivateCachingTerminologyProviderDecorator;
import org.opencds.cqf.cql.evaluator.engine.terminology.PriorityTerminologyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to construct a CqlEvaluator that may have multiple
 * content, data, and terminology sources NOTE: The CqlEngine and
 * RetrieveProviders use terminology at two separate levels. The CqlEngine uses
 * a TerminologyProvider directly to evaluate CQL that deals with Terminology,
 * and RetrieveProviders can also use a TerminologyProvider to support filtering
 * during retrieves. The configuration of RetrieveProviders is done via the
 * RetrieveProviderConfigurer class. An instance of one must be specified if you
 * wish the RetrieveProviders to be configured to use the constructed
 * multi-source TerminologyProvider
 */
@Named
public class CqlEvaluatorBuilder {

    private static Logger logger = LoggerFactory.getLogger(CqlEvaluatorBuilder.class);

    private List<LibraryLoader> libraryLoaders;
    private List<TerminologyProvider> terminologyProviders;

    private Map<String, Pair<ModelResolver, List<RetrieveProvider>>> dataProviderParts;

    private RetrieveProviderConfigurer retrieveProviderConfigurer;
    private EnumSet<CqlEngine.Options> engineOptions;

    /**
     * Constructor for the CqlEvaluator builder. This constructor applies
     * configuration to the CqlEvaluator that is built as specified by the supplied
     * arguments. The arguments may be null.
     * 
     * @param retrieveProviderConfigurer a RetrieveProviderConfigurer that will
     *                                   apply configuration to the
     *                                   RetrieveProviders
     */
    @Inject
    public CqlEvaluatorBuilder(RetrieveProviderConfigurer retrieveProviderConfigurer) {
        this(retrieveProviderConfigurer, null);
    }

    /**
     * Constructor for the CqlEvaluator builder. This constructor applies
     * configuration to the CqlEvaluator that is built as specified by the supplied
     * arguments. The arguments may be null.
     * 
     * @param retrieveProviderConfigurer a RetrieveProviderConfigurer that will
     *                                   apply configuration to the
     *                                   RetrieveProviders
     * @param engineOptions              CQLEngine options to use for the underlying
     *                                   CQLEngine
     */
    public CqlEvaluatorBuilder(RetrieveProviderConfigurer retrieveProviderConfigurer,
            EnumSet<CqlEngine.Options> engineOptions) {
        this.retrieveProviderConfigurer = retrieveProviderConfigurer;
        this.engineOptions = engineOptions;
        this.libraryLoaders = new ArrayList<>();
        this.terminologyProviders = new ArrayList<>();
        this.dataProviderParts = new HashMap<>();
    }

    /**
     * Adds a LibraryLoader to the list of library loaders that may be used during
     * evaluation. This function uses FILO semantics. The first LibraryLoader added
     * is the last to be searched for a Library.
     * 
     * @param libraryLoader the LibraryLoader to add to the evaluation context
     * @return this CqlEvaluatorBuilder
     */
    public CqlEvaluatorBuilder withLibraryLoader(LibraryLoader libraryLoader) {
        requireNonNull(libraryLoader, "libraryLoader can not be null");

        this.libraryLoaders.add(libraryLoader);
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

            if (this.retrieveProviderConfigurer != null) {
                for (RetrieveProvider provider : providers) {
                    this.retrieveProviderConfigurer.configure(provider, terminologyProvider);
                }
            }

            dataProviders.put(entry.getKey(),
                    this.decorate(new CompositeDataProvider(modelResolver, new PriorityRetrieveProvider(providers))));
        }

        return dataProviders;
    }

    protected DataProvider decorate(DataProvider dataProvider) {
        return dataProvider;
    }

    protected TerminologyProvider decorate(TerminologyProvider terminologyProvider) {
        return new PrivateCachingTerminologyProviderDecorator(terminologyProvider);
    }

    /**
     * Builds a CqlEvaluator that uses all content, data, terminology sources
     * supplied, and has the appropriate configuration applied.
     * 
     * NOTE: The CqlEvaluator created by this default implementation
     * is meant to be short-lived (e.g. for the duration of a request).
     * It won't pick up changes to underlying content.
     * 
     * @return a CqlEvaluator
     */
    public CqlEvaluator build() {
        Collections.reverse(this.libraryLoaders);
        LibraryLoader libraryLoader = new PriorityLibraryLoader(libraryLoaders);

        Collections.reverse(this.terminologyProviders);
        TerminologyProvider terminologyProvider = this.decorate(new PriorityTerminologyProvider(terminologyProviders));

        Map<String, DataProvider> dataProviders = this.buildDataProviders(terminologyProvider);

        return new CqlEvaluator(libraryLoader, dataProviders, terminologyProvider, this.engineOptions);
    }

}
