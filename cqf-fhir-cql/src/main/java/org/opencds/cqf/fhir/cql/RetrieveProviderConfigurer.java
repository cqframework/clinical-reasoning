package org.opencds.cqf.fhir.cql;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

public interface RetrieveProviderConfigurer {
    public void configure(RetrieveProvider retrieveProvider, TerminologyProvider terminologyProvider);
}
