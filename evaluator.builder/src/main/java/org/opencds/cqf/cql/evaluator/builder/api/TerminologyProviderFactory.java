package org.opencds.cqf.cql.evaluator.builder.api;

import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;

public interface TerminologyProviderFactory {
    public TerminologyProvider create(EndpointInfo endpointInfo);
    // public TerminologyProvider create(IBaseBundle bundle);
}