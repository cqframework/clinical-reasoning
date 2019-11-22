package com.alphora.cql.service.factory;

import org.opencds.cqf.cql.terminology.TerminologyProvider;

public interface TerminologyProviderFactory {
    TerminologyProvider create(String model, String version, String terminologyUri);
}