package org.opencds.cqf.cql.evaluator.builder;

import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;

public class DataProviderComponents {

    protected String modelUri;
    protected ModelResolver modelResolver;
    protected RetrieveProvider retrieveProvider;

    public DataProviderComponents(String modelUri, ModelResolver modelResolver, RetrieveProvider retrieveProvider) {
        this.modelUri = modelUri;
        this.modelResolver = modelResolver;
        this.retrieveProvider = retrieveProvider;
    }
    
    public String getModelUri() {
        return this.modelUri;
    }

    public ModelResolver getModelResolver() {
        return this.modelResolver;
    }

    public RetrieveProvider getRetrieveProvider() {
        return this.retrieveProvider;
    }
}
