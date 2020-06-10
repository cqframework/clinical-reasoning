package org.opencds.cqf.cql.evaluator.builder.context;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.context.api.DataContext;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleDataProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileDataProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.remote.RemoteDataProviderBuilder;

import org.apache.commons.lang3.NotImplementedException;

// File, Remote, Resource, ...String?
/**
 * Provides DataContext needed for CQL Evaluation
 */
public class BuilderDataContext extends BuilderContext implements DataContext {

    /**
     * set TerminologyProvider with Preconfigured TerminologyProvider
     * 
     * @param terminologyProvider preconfigured terminology provider
     */
    public void setTerminologyProvider(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    @Override
    public CqlEvaluatorBuilder withPreConfiguredDataProvider(Map<String, DataProvider> dataProviderMap) {
        return asCqlEvaluatorBuilder(this);
    }

    @Override
    public CqlEvaluatorBuilder withFileDataProvider(Map<String, String> modelUrlMap) {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(terminologyProvider);
        this.dataProviderMap = fileDataProviderBuilder.build(this.models, modelUrlMap);
        return asCqlEvaluatorBuilder(this);
    }

    @Override
    // Should this come from Library models?
    public CqlEvaluatorBuilder withBundleDataProvider(IBaseBundle dataBundle) {
        BundleDataProviderBuilder bundleDataProviderBuilder = new BundleDataProviderBuilder(terminologyProvider);
        this.dataProviderMap = bundleDataProviderBuilder.build(dataBundle);
        return asCqlEvaluatorBuilder(this);
    }

    @Override
    public CqlEvaluatorBuilder withRemoteDataProvider(List<URL> urlList)
            throws IOException, InterruptedException, URISyntaxException {
        RemoteDataProviderBuilder remoteDataProviderBuilder = new RemoteDataProviderBuilder(terminologyProvider);
        this.dataProviderMap = remoteDataProviderBuilder.build(urlList, this.clientFactory);
        return asCqlEvaluatorBuilder(this);
    }

    @Override
    public CqlEvaluatorBuilder withStringDataProvider(List<String> uriList) {
        throw new NotImplementedException("String Representations of data not yet implemented.");
    }

    private CqlEvaluatorBuilder asCqlEvaluatorBuilder(BuilderContext thisBuilderContext) {
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        // This is a hack for now (figure out casting)
        cqlEvaluatorBuilder.libraryLoader = thisBuilderContext.libraryLoader;
        cqlEvaluatorBuilder.dataProviderMap = thisBuilderContext.dataProviderMap;
        cqlEvaluatorBuilder.terminologyProvider = thisBuilderContext.terminologyProvider;
        cqlEvaluatorBuilder.models = thisBuilderContext.models;
        cqlEvaluatorBuilder.clientFactory = thisBuilderContext.clientFactory;
        cqlEvaluatorBuilder.setEngineOptions(thisBuilderContext.getEngineOptions());
        cqlEvaluatorBuilder.parameterDeserializer = thisBuilderContext.parameterDeserializer;
        cqlEvaluatorBuilder.setTranslatorOptions(thisBuilderContext.getTranslatorOptions());
        return cqlEvaluatorBuilder;
    }
}