package org.opencds.cqf.cql.evaluator.builder.context;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Options;
import org.opencds.cqf.cql.evaluator.resolver.ParameterResolver;

/**
 * Provides any Context needed for CQL Evaluation
 */
public abstract class BuilderContext {

    protected TerminologyProvider terminologyProvider;
    protected LibraryLoader libraryLoader;
    protected Map<String, DataProvider> dataProviderMap;
    protected List<ModelInfo> models;

    private EnumSet<Options> options = EnumSet.noneOf(Options.class);

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
     * get Options
     * @return Options
     */
    public EnumSet<Options> getOptions() {
        return this.options;
    }

    /**
     * Set Options
     * @param options options to be used
     */
    public void setOptions(EnumSet<Options> options) {
        Objects.requireNonNull(options, "options can not be null.");
        this.options = options;
    }

    /**
     * get Engine Options
     * @return Engine Options
     */
	public EnumSet<org.opencds.cqf.cql.engine.execution.CqlEngine.Options> getEngineOptions() {
        //todo engine options
		return null;
	}

    /**
     * 
     * @return resolved parameters
     */
	public ParameterResolver getPameterResolver() {
		return null;
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
}