package org.opencds.cqf.cql.evaluator.builder.implementation.string;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

public class StringDataProviderBuilder {
    private TerminologyProvider terminologyProvider;

    public StringDataProviderBuilder(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    public Map<String, DataProvider> build(Map<String, Pair<String, String>> models, List<String> dataBundles) {
		throw new NotImplementedException("String Representations of Data Bundles is not yet supported.");
	}
}