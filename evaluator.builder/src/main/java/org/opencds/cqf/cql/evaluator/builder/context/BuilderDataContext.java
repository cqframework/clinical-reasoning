package org.opencds.cqf.cql.evaluator.builder.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.Helpers;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.context.api.DataContext;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleDataProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileDataProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.remote.RemoteDataProviderBuilder;

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
    public CqlEvaluatorBuilder withFileDataProvider(List<ModelInfo> models) {
        boolean fileUri = true;

        for (ModelInfo model : models) {
            if (!Helpers.isFileUri(model.getUrl())) {
                fileUri = true;
            }
            else {
                fileUri = false;
            }
            if(fileUri) {
                FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(terminologyProvider);
                fileDataProviderBuilder.build(models);
            }
            else {
                RemoteDataProviderBuilder remoteDataProviderBuilder = new RemoteDataProviderBuilder(terminologyProvider);
                //remoteDataProviderBuilder.build(modelInfos, terminologyProvider, clientBuilder);
            }
            
        }
        return asCqlEvaluatorBuilder(this);
    }

    // @Override
    // public CqlEvaluatorBuilder withDataProviderUsingModelInfo() {
    //     FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(terminologyProvider);
    //     fileDataProviderBuilder.build(modelInfoList);
    //     return asCqlEvaluatorBuilder(this);
    // }

    // @Override
    // public CqlEvaluatorBuilder withDataProvider(List<String> uriList, IBaseBundle bundle) {
    //     BundleDataProviderBuilder bundleDataProviderBuilder = new BundleDataProviderBuilder(terminologyProvider);
    //     for (String uri : uriList) {
    //         if (!modelInfos.stream().filter(modelInfo -> modelInfo.getUrl().contains(uri)).findFirst().isPresent()) {
    //             throw new IllegalArgumentException(String.format("Unknown data provider uri: %s", uri));
    //         }
    //     }
    //     bundleDataProviderBuilder.build(modelInfos, bundle);
    //     return asCqlEvaluatorBuilder(this);
    // }

    @Override
    public CqlEvaluatorBuilder withBundleDataProvider(Map<ModelInfo, IBaseBundle> modelVersionBundleMap) {
        BundleDataProviderBuilder bundleDataProviderBuilder = new BundleDataProviderBuilder(terminologyProvider);
        bundleDataProviderBuilder.build(modelVersionBundleMap);
        return asCqlEvaluatorBuilder(this);
    }

    // public BuilderContext withDataProvider(Map<String, List<String>> modelBundleStrings) {
    //     return asCqlEvaluatorBuilder(this);
    // }

    private CqlEvaluatorBuilder asCqlEvaluatorBuilder(BuilderContext thisBuilderContext) {
        CqlEvaluatorBuilder cqlEvaluatorBuilder = (CqlEvaluatorBuilder)thisBuilderContext;
        return cqlEvaluatorBuilder;
    }
}