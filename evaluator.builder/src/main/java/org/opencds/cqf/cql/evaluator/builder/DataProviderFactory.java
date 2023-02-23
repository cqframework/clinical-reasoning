package org.opencds.cqf.cql.evaluator.builder;

import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface DataProviderFactory {
    /*
     * Returns components for a DataProvider for the given endpointInfo. The correct FHIR version is
     * detected based on the endpointInfo. Returns null if endpointInfo is null.
     *
     * Override in subclasses to provide "default" behavior for your platform.
     *
     * @param endpointInfo the EndpointInfo for the location of data
     *
     * @return a DataProviderComponents containing a model url, a ModelResolver, and a
     * RetrieveProvider
     */
    public DataProviderComponents create(EndpointInfo endpointInfo);

    /**
     * Returns components for a DataProvider for the given Bundle. The correct FHIR version is
     * detected based on the the Bundle. Returns null if dataBundle is null.
     *
     * @param dataBundle the Bundle to use for data
     * @return a DataProviderComponents containing a model url, a ModelResolver, and a
     *         RetrieveProvider
     */
    public DataProviderComponents create(IBaseBundle dataBundle);
}
