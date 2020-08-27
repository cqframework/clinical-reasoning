package org.opencds.cqf.cql.evaluator.builder.api;

import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.api.model.DataProviderConfig;

public interface DataProviderConfigurer {
    public void configure(DataProvider dataProvider, DataProviderConfig dataProviderConfig);
}