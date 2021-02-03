package org.opencds.cqf.cql.evaluator.builder.data;

import java.util.List;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;

public interface TypedRetrieveProviderFactory {

    public String getType();

    public RetrieveProvider create(String url, List<String> headers);
}