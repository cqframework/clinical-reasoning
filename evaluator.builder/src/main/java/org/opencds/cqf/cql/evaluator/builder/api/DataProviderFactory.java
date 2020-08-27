package org.opencds.cqf.cql.evaluator.builder.api;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;

public interface DataProviderFactory {
    public Pair<String, DataProvider> create(EndpointInfo endpointInfo);

    // public Pair<String, DataProvider> create(IBaseBundle bundle);
}