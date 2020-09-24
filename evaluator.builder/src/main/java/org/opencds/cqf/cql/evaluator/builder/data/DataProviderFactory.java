package org.opencds.cqf.cql.evaluator.builder.data;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.NoOpRetrieveProvider;

import ca.uhn.fhir.context.FhirContext;

public class DataProviderFactory implements org.opencds.cqf.cql.evaluator.builder.DataProviderFactory {

    private FhirContext fhirContext;
    private Set<ModelResolverFactory> modelResolverFactories;
    private Set<RetrieveProviderFactory> retrieveProviderFactories;

    @Inject
    public DataProviderFactory(FhirContext fhirContext, Set<ModelResolverFactory> modelResolverFactories,
            Set<RetrieveProviderFactory> retrieveProviderFactories) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.modelResolverFactories = Objects.requireNonNull(modelResolverFactories,
                "modelResolverFactory can not be null");
        this.retrieveProviderFactories = Objects.requireNonNull(retrieveProviderFactories,
                "retrieveResolverFactory can not be null");

    }

    @Override
    public Triple<String, ModelResolver, RetrieveProvider> create(EndpointInfo endpointInfo) {
        Objects.requireNonNull(endpointInfo, "endpointInfo can not be null");

        if (endpointInfo.getType() == null) {
            endpointInfo.setType(detectType(endpointInfo.getAddress()));
        }

        String modelUri = detectModel(endpointInfo.getAddress(), endpointInfo.getType());
        Pair<ModelResolver, RetrieveProvider> dp = create(modelUri, endpointInfo.getType(), endpointInfo.getAddress(),
                endpointInfo.getHeaders());

        return Triple.of(modelUri, dp.getLeft(), dp.getRight());
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

        throw new IllegalArgumentException(
                String.format("no registered ModelResolverFactory for modelUri: %s", modelUri));
    }

    protected Pair<ModelResolver, RetrieveProvider> create(String modelUri, IBaseCoding connectionType, String url, List<String> headers) {
        ModelResolver modelResolver = this.getFactory(modelUri)
                .create(this.fhirContext.getVersion().getVersion().getFhirVersionString());

        RetrieveProvider retrieveProvider = null;
        if (url == null || connectionType == null) {
            retrieveProvider = new NoOpRetrieveProvider();
        } else {

            for (RetrieveProviderFactory factory : this.retrieveProviderFactories) {
                if (connectionType.getCode().equals(factory.getType())) {
                    retrieveProvider = factory.create(url, headers);
                    break;
                }
            }

            if (retrieveProvider == null) {
                throw new IllegalArgumentException(
                        String.format("unknown connectionType for loading FHIR Resources: %s", connectionType));
            }
        }

        return Pair.of(modelResolver, retrieveProvider);
    }

    @Override
    public Triple<String, ModelResolver, RetrieveProvider> create(IBaseBundle dataBundle) {
        Objects.requireNonNull(dataBundle, "dataBundle can not be null");

        if (!dataBundle.getStructureFhirVersionEnum().equals(this.fhirContext.getVersion().getVersion())) {
            throw new IllegalArgumentException("The FHIR version of dataBundle and the FHIR context do not match");
        }

        ModelResolver modelResolver = this.getFactory(Constants.FHIR_MODEL_URI)
                .create(this.fhirContext.getVersion().getVersion().getFhirVersionString());

        return Triple.of(Constants.FHIR_MODEL_URI, modelResolver, new BundleRetrieveProvider(fhirContext, dataBundle));
    }
}