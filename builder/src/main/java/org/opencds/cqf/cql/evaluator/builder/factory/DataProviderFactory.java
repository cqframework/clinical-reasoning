package org.opencds.cqf.cql.evaluator.factory;

import java.util.List;
import java.util.Map;

import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.evaluator.ModelInfo;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

public interface DataProviderFactory {
    Map<String, DataProvider> create(List<ModelInfo> models, TerminologyProvider terminologyProvider);
    Map<String, DataProvider> create(List<ModelInfo> models, TerminologyProvider terminologyProvider, ClientFactory clientFactory);
}