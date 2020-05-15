package org.opencds.cqf.cql.evaluator.builder.context.interface;

import java.util.List;
import java.util.Map;

import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;

public interface DataContext {
    public CqlEvaluatorBuilder withDataProvider(Map<String, DataProvider> dataProviderMap);
    public CqlEvaluatorBuilder withDataProvider(List<String> uriList);
    public CqlEvaluatorBuilder withDataProviderUsingModelInfo(List<ModelInfo> modelInfoList);
    public CqlEvaluatorBuilder withDataProvider(List<String> uriList, IBaseBundle bundle);
    public CqlEvaluatorBuilder withDataProviderUsingModelInfo(List<ModelInfo> modelInfoList, IBaseBundle bundle);
}
