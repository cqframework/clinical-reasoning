package org.opencds.cqf.cql.evaluator.builder.context.api;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;

public interface DataContext {
    public CqlEvaluatorBuilder withPreConfiguredDataProvider(Map<String, DataProvider> dataProviderMap);
    public CqlEvaluatorBuilder withRemoteDataProvider(List<URL> urlList) throws IOException, InterruptedException, URISyntaxException;
    public CqlEvaluatorBuilder withFileDataProvider(Map<String, String> modelUrlMap);
    public CqlEvaluatorBuilder withStringDataProvider(List<String> uriList);
    public CqlEvaluatorBuilder withBundleDataProvider(IBaseBundle dataBundle);
}
