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
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringDataProviderBuilder;
/**
 * Provides DataContext needed for CQL Evaluation
 *    1. A pre-constructed DataProvider Map
 *    2. String representations of Data Resources
 *    3. A remote data repository
 *    4. A filesystem with data content
 *    5. Bundles containing FHIR Data
 */
public class BuilderDataContext extends BuilderContext implements DataContext {

    /**
     * set TerminologyProvider with Preconfigured TerminologyProvider, if not set TerminologyProvider is considered null.
     * 
     * @param terminologyProvider preconfigured terminology provider
     */
    public void setTerminologyProvider(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    /**
     * set DataProviders with Preconfigured DataProvider Map, the maps a given Model uri to the DataProvider used to execute
     * 
     * @param dataProviderMap preconfigured DataProviders map
     * @return CqlEvaluatorBuilder a new instance with the appropriate context filled out.
     */
    @Override
    public CqlEvaluatorBuilder withPreConfiguredDataProvider(Map<String, DataProvider> dataProviderMap) {
        return asCqlEvaluatorBuilder(this);
    }

    /**
     * set DataProviders from a Map of a given Model URL to the appropriate File URI
     * 
     * @param modelUrlMap model to URI map
     * @return CqlEvaluatorBuilder a new instance with the appropriate context filled out.
     */
    @Override
    public CqlEvaluatorBuilder withFileDataProvider(Map<String, String> modelUrlMap) {
        FileDataProviderBuilder fileDataProviderBuilder = new FileDataProviderBuilder(terminologyProvider);
        this.dataProviderMap = fileDataProviderBuilder.build(this.models, modelUrlMap);
        return asCqlEvaluatorBuilder(this);
    }

    /**
     * set DataProviders from a given Bundle containing the data used for execution
     * 
     * @param dataBundle FHIR Bundle containing Data
     * @return CqlEvaluatorBuilder a new instance with the appropriate context filled out.
     */
    @Override
    public CqlEvaluatorBuilder withBundleDataProvider(IBaseBundle dataBundle) {
        BundleDataProviderBuilder bundleDataProviderBuilder = new BundleDataProviderBuilder(terminologyProvider);
        this.dataProviderMap = bundleDataProviderBuilder.build(dataBundle);
        return asCqlEvaluatorBuilder(this);
    }

    /**
     * set DataProviders from a List of URLs needed to gather the data used for execution
     * If now ClientFactory is provided a DefaultClientFactory will be used.
     * Must be a URL of a HAPI FHIR Client as of now.
     * 
     * @param urlList urls needed for execution
     * @return CqlEvaluatorBuilder a new instance with the appropriate context filled out.
     */
    @Override
    public CqlEvaluatorBuilder withRemoteDataProvider(List<URL> urlList)
            throws IOException, InterruptedException, URISyntaxException {
        RemoteDataProviderBuilder remoteDataProviderBuilder = new RemoteDataProviderBuilder(terminologyProvider);
        this.dataProviderMap = remoteDataProviderBuilder.build(urlList, this.clientFactory);
        return asCqlEvaluatorBuilder(this);
    }

    /**
     * set DataProviders from String representations of the data used for execution
     * !!!! This API is not supported yet !!!!
     * @param dataBundles representing Data
     * @return CqlEvaluatorBuilder a new instance with the appropriate context filled out.
     */
    @Override
    public CqlEvaluatorBuilder withStringDataProvider(List<String> dataBundles) {
        StringDataProviderBuilder stringDataProviderBuilder = new StringDataProviderBuilder(terminologyProvider);
        this.dataProviderMap = stringDataProviderBuilder.build(models, dataBundles);
        return asCqlEvaluatorBuilder(this);
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