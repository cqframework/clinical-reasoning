package org.opencds.cqf.cql.evaluator.builder.api;

import java.util.Collection;

import org.opencds.cqf.cql.engine.data.DataProvider;

public interface DataProviderExtender {
    public void extend(DataProvider target, Collection<DataProvider> otherProviders);
}