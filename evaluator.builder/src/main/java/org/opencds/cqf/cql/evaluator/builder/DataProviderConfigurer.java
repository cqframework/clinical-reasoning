package org.opencds.cqf.cql.evaluator.builder;

import org.opencds.cqf.cql.engine.data.DataProvider;

public interface DataProviderConfigurer {
    public void configure(DataProvider dataProvider, DataProviderConfig dataProviderConfig);
}