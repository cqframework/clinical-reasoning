package org.opencds.cqf.cql.evaluator.builder;

import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

public interface TerminologyProviderFactory {
    public TerminologyProvider create(EndpointInfo endpointInfo);
    // public TerminologyProvider create(IBaseBundle bundle);
}