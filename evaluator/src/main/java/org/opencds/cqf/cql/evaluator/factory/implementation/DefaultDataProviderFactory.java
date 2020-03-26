package org.opencds.cqf.cql.evaluator.factory.implementation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencds.cqf.cql.evaluator.Helpers;
import org.opencds.cqf.cql.evaluator.ModelInfo;
import org.opencds.cqf.cql.evaluator.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.factory.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.provider.FileBasedFhirRetrieveProvider;
import org.opencds.cqf.cql.evaluator.provider.NoOpRetrieveProvider;

import org.apache.commons.lang3.NotImplementedException;
import org.opencds.cqf.cql.data.CompositeDataProvider;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.model.ModelResolver;
import org.opencds.cqf.cql.model.R4FhirModelResolver;
import org.opencds.cqf.cql.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class DefaultDataProviderFactory implements DataProviderFactory {

    @Override
    public Map<String, DataProvider> create(List<ModelInfo> models,
            TerminologyProvider terminologyProvider) {
        return this.create(models, terminologyProvider, null);
    }

    @Override
    public Map<String, DataProvider> create(List<ModelInfo> models, TerminologyProvider terminologyProvider,
            ClientFactory clientFactory) {
            return this.getProviders(models, terminologyProvider, clientFactory);
    }

    private Map<String, DataProvider> getProviders(List<ModelInfo> models,
            TerminologyProvider terminologyProvider, ClientFactory clientFactory) {
        Map<String, DataProvider> providers = new HashMap<>();
        for (ModelInfo m : models) {
            providers.put(m.getName(),
                    this.getProvider(m.getName(), m.getVersion(), m.getUrl(), terminologyProvider, clientFactory));
        }

        return providers;
    }

    private DataProvider getProvider(String model, String version, String uri,
            TerminologyProvider terminologyProvider, ClientFactory clientFactory) {
        switch (model) {
        case "http://hl7.org/fhir":
            return this.getFhirProvider(version, uri, terminologyProvider, clientFactory);

        case "urn:healthit-gov:qdm:v5_4":
            return this.getQdmProvider(version, uri, terminologyProvider);

        default:
            throw new IllegalArgumentException(String.format("Unknown data provider uri: %s", model));
        }
    }

    private DataProvider getFhirProvider(String version, String uri, TerminologyProvider terminologyProvider, ClientFactory clientFactory) {
        FhirContext context;
        ModelResolver modelResolver;
        RetrieveProvider retrieveProvider;
        switch (version) {
            case "2.0.0":
                context = FhirContext.forDstu2_1();
                modelResolver = new Dstu2FhirModelResolver();
                break;
            case "3.0.1":
            case "3.0.2":
            case "3.0.0":
                context = FhirContext.forDstu3();
                modelResolver = new Dstu3FhirModelResolver();
                break;
            case "4.0.0":
                context = FhirContext.forR4();
                modelResolver = new R4FhirModelResolver();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown FHIR data provider version: %s", version));
        }
        
        if (uri == null) {
            retrieveProvider = new NoOpRetrieveProvider();
        }
        else if (Helpers.isFileUri(uri)) {
            retrieveProvider = new FileBasedFhirRetrieveProvider(uri, terminologyProvider, context, modelResolver);
        } else {    
            if (clientFactory == null) {
                throw new IllegalArgumentException(String.format("Needed to access remote url %s and ClientFactory was null."));
            }
            IGenericClient client = clientFactory.create(uri);
            RestFhirRetrieveProvider fhirRetrieveProvider = new RestFhirRetrieveProvider(new SearchParameterResolver(context), client);
            fhirRetrieveProvider.setTerminologyProvider(terminologyProvider);
            fhirRetrieveProvider.setExpandValueSets(true);
            retrieveProvider = fhirRetrieveProvider;        
        }
            
        return new CompositeDataProvider(modelResolver, retrieveProvider);
    }

    private DataProvider getQdmProvider(String version, String uri, TerminologyProvider terminologyProvider) {
        throw new NotImplementedException("QDM data providers are not yet implemented");
    }
}