package org.opencds.cqf.cql.evaluator.builder.terminology;

import java.util.List;

import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

public interface TypedTerminologyProviderFactory {
    public String getType();

    public TerminologyProvider create(String url, List<String> headers);
}
