package org.opencds.cqf.cql.evaluator.builder;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

public interface TerminologyProviderFactory {
    /**
     * Returns a TerminologyProvider for the given endpointInfo. Returns null if
     * endpointInfo is null.
     * 
     * Override in subclasses to provide "default" behavior for your platform.
     * 
     * @param endpointInfo the EndpointInfo for the location of terminology
     * @return a TerminologyProvider
     */
    public TerminologyProvider create(EndpointInfo endpointInfo);

    /**
     * Returns a TerminologyProvider for the given Bundle. Returns null if
     * terminologyBundle is null.
     * 
     * @param terminologyBundle the Bundle to use for terminology
     * @return a TerminologyProvider
     */
    public TerminologyProvider create(IBaseBundle terminologyBundle);
}