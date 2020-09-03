package org.opencds.cqf.cql.evaluator.builder.common;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.engine.data.ExtensibleDataProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.NoOpRetrieveProvider;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class DataProviderFactory implements org.opencds.cqf.cql.evaluator.builder.DataProviderFactory {

    private FhirContext fhirContext;
    private Set<ModelResolverFactory> modelResolverFactories;
    private ClientFactory clientFactory;
    private DirectoryBundler directoryBundler;

    @Inject
    public DataProviderFactory(FhirContext fhirContext, 
        Set<ModelResolverFactory> modelResolverFactories, ClientFactory clientFactory, DirectoryBundler directoryBundler) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.modelResolverFactories = Objects.requireNonNull(modelResolverFactories, "modelResolverFactory can not be null");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory can not be null");
        this.directoryBundler = Objects.requireNonNull(directoryBundler, "directoryBundler can not be null");
    }

    @Override
    public Pair<String, DataProvider> create(EndpointInfo endpointInfo) {
        Objects.requireNonNull(endpointInfo, "endpointInfo can not be null");

        if (endpointInfo.getType() == null) {
            endpointInfo.setType(detectType(endpointInfo.getAddress()));
        }

        String modelUri = detectModel(endpointInfo.getAddress(), endpointInfo.getType());
        DataProvider dp = create(modelUri, endpointInfo.getAddress(), endpointInfo.getType(), endpointInfo.getHeaders());

        return Pair.of(modelUri, dp);
    }

    public IBaseCoding detectType(String url) {
        if (url == null) {
            return null;
        }

        if (isFileUri(url)) {
            return Constants.HL7_FHIR_FILES_CODE;
        } else {
            return Constants.HL7_FHIR_REST_CODE;
        }
    }

    public String detectModel(String url, IBaseCoding connectionType) {
        switch (connectionType.getCode()) {
            case Constants.HL7_FHIR_FILES:
            case Constants.HL7_FHIR_REST:
                return Constants.FHIR_MODEL_URI;
            default:
                return null;
        }
    }

    protected ModelResolverFactory getFactory(String modelUri) {
        for (ModelResolverFactory factory : this.modelResolverFactories) {
            if (factory.getModelUri().equals(modelUri)) {
                return factory;
            }
        }

        throw new IllegalArgumentException(String.format("no registered ModelResolverFactory for modelUri: %s", modelUri));
    }

    protected ExtensibleDataProvider create(String modelUri, String url, IBaseCoding connectionType, List<String> headers) {
        ModelResolver modelResolver = this.getFactory(modelUri)
            .create(this.fhirContext.getVersion().getVersion().getFhirVersionString());

        RetrieveProvider retrieveProvider = null;
        if (url == null || connectionType == null) {
            retrieveProvider = new NoOpRetrieveProvider();
        } else {
            switch (connectionType.getCode()) {
                case Constants.HL7_FHIR_REST:
                    IGenericClient client = this.clientFactory.create(url, headers);
                    retrieveProvider = new RestFhirRetrieveProvider(
                            new SearchParameterResolver(this.fhirContext), client);
                    break;
                case Constants.HL7_FHIR_FILES:
                    retrieveProvider = new BundleRetrieveProvider(this.fhirContext, modelResolver,
                            this.directoryBundler.bundle(url));
                    break;
                default:
                    throw new IllegalArgumentException(String.format("invalid connectionType for loading FHIR Resources: %s", connectionType));
            }
        }

        return new ExtensibleDataProvider(modelResolver, retrieveProvider);
    }

    // @Override
    // public Pair<String, DataProvider> create(IBaseBundle bundle) {
    //     Objects.requireNonNull(bundle, "bundle can not be null");
    //     ModelResolver modelResolver = this.getFactory(Constants.FHIR_URI)
    //         .create(this.fhirContext.getVersion().getVersion().getFhirVersionString());
    //     RetrieveProvider retrieveProvider = new BundleRetrieveProvider(this.fhirContext, modelResolver,
    //                         bundle);

    //     return Pair.of(Constants.FHIR_URI, new ExtensibleDataProvider(modelResolver, retrieveProvider));
    //}
}