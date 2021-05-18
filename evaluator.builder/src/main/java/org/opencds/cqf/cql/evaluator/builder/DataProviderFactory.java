package org.opencds.cqf.cql.evaluator.builder;

import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;

public interface DataProviderFactory {
    /**
     * Returns components for a DataProvider for the given endpointInfo. The correct FHIR version is detected based on
     * the endpointInfo. Returns
     * null if endpointInfo is null. 
     * 
     * Override in subclasses to provide "default" behavior for your platform.
     * 
     * @param endpointInfo the EndpointInfo for the location of data
     * @return a Triple containing a model url, a ModelResolver, and a
     *         RetrieveProvider
     */
    public Triple<String, ModelResolver, RetrieveProvider> create(EndpointInfo endpointInfo);

    /**
     * Returns components for a DataProvider for the given Bundle. The correct FHIR
     * version is detected based on the the Bundle. Returns
     * null if dataBundle is null. 
     * 
     * @param dataBundle the Bundle to use for data
     * @return a Triple containing a model url, a ModelResolver, and a
     *         RetrieveProvider
     */
    public Triple<String, ModelResolver, RetrieveProvider> create(IBaseBundle dataBundle);
}