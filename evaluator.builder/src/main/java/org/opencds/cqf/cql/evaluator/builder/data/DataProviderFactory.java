package org.opencds.cqf.cql.evaluator.builder.data;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

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

@Named
public class DataProviderFactory implements org.opencds.cqf.cql.evaluator.builder.DataProviderFactory {

    private FhirContext fhirContext;
    private Set<ModelResolverFactory> modelResolverFactories;
    private Set<TypedRetrieveProviderFactory> retrieveProviderFactories;

    @Inject
    public DataProviderFactory(FhirContext fhirContext, Set<ModelResolverFactory> modelResolverFactories,
            Set<TypedRetrieveProviderFactory> retrieveProviderFactories) {
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.modelResolverFactories = requireNonNull(modelResolverFactories, "modelResolverFactory can not be null");
        this.retrieveProviderFactories = requireNonNull(retrieveProviderFactories,
                "retrieveResolverFactory can not be null");

    }

    @Override
    public Triple<String, ModelResolver, RetrieveProvider> create(EndpointInfo endpointInfo) {
        if (endpointInfo == null) {
            return null;
        }

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
        if (connectionType == null) {
            return null;
        }
        
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

    protected Pair<ModelResolver, RetrieveProvider> create(String modelUri, IBaseCoding connectionType, String url,
            List<String> headers) {
        ModelResolver modelResolver = this.getFactory(modelUri)
                .create(this.fhirContext.getVersion().getVersion().getFhirVersionString());

        RetrieveProvider retrieveProvider = null;
        if (url == null || connectionType == null) {
            retrieveProvider = new NoOpRetrieveProvider();
        } else {

            for (TypedRetrieveProviderFactory factory : this.retrieveProviderFactories) {
                if (connectionType.getCode().equals(factory.getType())) {
                    retrieveProvider = factory.create(url, headers);
                    break;
                }
            }

            if (retrieveProvider == null) {
                throw new IllegalArgumentException(
                        String.format("unsupported or unknown connectionType for loading FHIR Resources: %s", connectionType));
            }
        }

        return Pair.of(modelResolver, retrieveProvider);
    }

    @Override
    public Triple<String, ModelResolver, RetrieveProvider> create(IBaseBundle dataBundle) {
        if (dataBundle == null) {
            return null;
        }

        if (!dataBundle.getStructureFhirVersionEnum().equals(this.fhirContext.getVersion().getVersion())) {
            throw new IllegalArgumentException("The FHIR version of dataBundle and the FHIR context do not match");
        }

        ModelResolver modelResolver = this.getFactory(Constants.FHIR_MODEL_URI)
                .create(this.fhirContext.getVersion().getVersion().getFhirVersionString());

        return Triple.of(Constants.FHIR_MODEL_URI, modelResolver, new BundleRetrieveProvider(fhirContext, dataBundle));
    }
}