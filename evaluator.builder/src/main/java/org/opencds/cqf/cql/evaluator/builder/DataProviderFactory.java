package org.opencds.cqf.cql.evaluator.builder;

import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;

public interface DataProviderFactory {
    public Triple<String, ModelResolver, RetrieveProvider> create(EndpointInfo endpointInfo);
    public Triple<String, ModelResolver, RetrieveProvider>  create(IBaseBundle dataBundle);
}