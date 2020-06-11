package org.opencds.cqf.cql.evaluator.builder.context;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.factory.DefaultClientFactory;
import org.opencds.cqf.cql.evaluator.resolver.ParameterDeserializer;
import org.opencds.cqf.cql.evaluator.resolver.implementation.DefaultParameterDeserializer;

/**
 * Provides any Context needed for CQL Evaluation
 * 1. build a LibraryLoader (required)
 * 2. build a TerminologyProvider (optional)
 * 3. build a DataProvider Map (optional)
 * 
 * Additional Options
 * Provide your own CqlTranslatorOptions
 * Provide your own ParameterDeserializer
 * Provide your own Engine Options
 * Provide your own ClientFactory 
 */
public abstract class BuilderContext {

    protected TerminologyProvider terminologyProvider;
    protected LibraryLoader libraryLoader;
    protected Map<String, DataProvider> dataProviderMap;
    protected Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
    protected ClientFactory clientFactory = new DefaultClientFactory();
    protected ParameterDeserializer parameterDeserializer;

    private CqlTranslatorOptions cqlTranslatorOptions = CqlTranslatorOptions.defaultOptions();
    // for the future provide some defaultOptions for the Engine
    EnumSet<org.opencds.cqf.cql.engine.execution.CqlEngine.Options> engineOptions = null;

    /**
     * Default BuilderContext
     */
    public BuilderContext() {

    }

    /**
     * Preconfigured TerminologyProvider
     * @param terminologyProvider terminologyProvider to be used
     */
    public BuilderContext(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    /**
     * set Translator Options
     * @param cqlTranslatorOptions to be used, will default if not specified
     */
    public void setTranslatorOptions(CqlTranslatorOptions cqlTranslatorOptions) {
        Objects.requireNonNull(cqlTranslatorOptions, "cqlTranslatorOptions can not be null.");
        this.cqlTranslatorOptions = cqlTranslatorOptions;
    }

    /**
     * get Translator Options
     * @return Translator Options
     */
	public CqlTranslatorOptions getTranslatorOptions() {
        return this.cqlTranslatorOptions;
    }
    
    /**
     * set Engine Options
     * @param engineOptions Engine Options to be used, null is acceptable
     */
    public void setEngineOptions(EnumSet<CqlEngine.Options> engineOptions) {
        // Objects.requireNonNull(engineOptions, "engineOptions can not be null."); Need to move default engine options in first
        this.engineOptions = engineOptions;
    }
    
    /**
     * get Engine Options
     * @return Engine Options
     */
	public EnumSet<org.opencds.cqf.cql.engine.execution.CqlEngine.Options> getEngineOptions() {
        return this.engineOptions;
	}

    /**
     *
     * @return default resolved parameters resolved parameters
     */
	public ParameterDeserializer getDefaultParameterDeserializer() {
		return new DefaultParameterDeserializer();
    }

    /**
     *
     * @return resolved parameters
     */
	public ParameterDeserializer getParameterDeserializer() {
		return this.parameterDeserializer;
    }

    /**
     *
     * @param parameterDeserializer to be used
     */
	public void setParameterDeserializer(ParameterDeserializer parameterDeserializer) {
		this.parameterDeserializer = parameterDeserializer;
    }

    /**
     * DataProvider needed for execution
     * @return Map of DataProviders to Model
     */
    public Map<String, DataProvider> getDataProvider() {
        return this.dataProviderMap;
    }

    /**
     * TerminologyProvider needed for execution
     * @return Terminology Provider
     */
	public TerminologyProvider getTerminologyProvider() {
		return this.terminologyProvider;
	}

    /**
     * LibraryLoader needed for execution
     * @return Library Loader
     */
	public LibraryLoader getLibraryLoader() {
		return this.libraryLoader;
    }

    /**
     * ClientFactory needed for execution
     * @return Client Factory
     */
    public ClientFactory getClientFactory() {
        return clientFactory;
    }

    /**
     *
     * @param clientFactory to be used
     */
    public void setClientFactory(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * models needed for execution
     * @return models populated by investigating the libraries provided.
     */
    public Map<String, Pair<String, String>> getModels() {
        return models;
    }
}