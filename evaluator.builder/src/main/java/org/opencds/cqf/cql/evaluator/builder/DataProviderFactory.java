package org.opencds.cqf.cql.evaluator.builder;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.engine.data.DataProvider;

public interface DataProviderFactory {
    public Pair<String, DataProvider> create(EndpointInfo endpointInfo);

    // public Pair<String, DataProvider> create(IBaseBundle bundle);
}