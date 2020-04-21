package org.opencds.cqf.cql.evaluator.builder.context;

import java.util.Map;

import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

public class BuilderDataContext {

    private TerminologyProvider terminologyProvider;
    public void setTerminologyProver(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    public Map<String, DataProvider> buildDataProviders() {
        return null;
    }
}