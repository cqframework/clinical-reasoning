package com.alphora.cql.service.factory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.alphora.cql.service.Helpers;
import com.alphora.cql.service.provider.FileBasedFhirRetrieveProvider;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.data.CompositeDataProvider;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.model.ModelResolver;
import org.opencds.cqf.cql.model.R4FhirModelResolver;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.retrieve.*;
import org.opencds.cqf.cql.searchparam.SearchParameterResolver;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.searchparam.registry.BaseSearchParamRegistry;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryDstu2;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryDstu3;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryR4;
import ca.uhn.fhir.jpa.subscription.module.standalone.FhirClientSearchParamProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;

// TODO: Dynamic data provider registration
public class DefaultDataProviderFactory implements DataProviderFactory {

    @Override
    public Map<String, DataProvider> create(Map<String, Pair<String, String>> modelVersionsAndUrls,
            TerminologyProvider terminologyProvider) {
        return this.getProviders(modelVersionsAndUrls, terminologyProvider);
    }

    private Map<String, DataProvider> getProviders(Map<String, Pair<String, String>> versions,
            TerminologyProvider terminologyProvider) {
        Map<String, DataProvider> providers = new HashMap<>();
        for (Map.Entry<String, Pair<String, String>> m : versions.entrySet()) {
            providers.put(m.getKey(),
                    this.getProvider(m.getKey(), m.getValue().getLeft(), m.getValue().getRight(), terminologyProvider));
        }

        return providers;
    }

    private DataProvider getProvider(String model, String version, String uri,
            TerminologyProvider terminologyProvider) {
        switch (model) {
        case "http://hl7.org/fhir":
            return this.getFhirProvider(version, uri, terminologyProvider);

        case "urn:healthit-gov:qdm:v5_4":
            return this.getQdmProvider(version, uri, terminologyProvider);

        default:
            throw new IllegalArgumentException(String.format("Unknown data provider uri: %s", model));
        }
    }

    private DataProvider getFhirProvider(String version, String uri, TerminologyProvider terminologyProvider) {
        FhirContext context;
        ModelResolver modelResolver;
        RetrieveProvider retrieveProvider;
        BaseSearchParamRegistry<?> searchParamRegistry;
        switch (version) {
            case "2.0.0":
                context = FhirContext.forDstu2_1();
                modelResolver = new Dstu2FhirModelResolver();
                searchParamRegistry = new SearchParamRegistryDstu2();
                break;
            case "3.0.0":
                context = FhirContext.forDstu3();
                modelResolver = new Dstu3FhirModelResolver();
                searchParamRegistry = new SearchParamRegistryDstu3();
                break;
            case "4.0.0":
                context = FhirContext.forR4();
                modelResolver = new R4FhirModelResolver();
                searchParamRegistry = new SearchParamRegistryR4();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown FHIR data provider version: %s", version));
        }    

        if (Helpers.isFileUri(uri)) {
            retrieveProvider = new FileBasedFhirRetrieveProvider(uri, terminologyProvider, context, modelResolver);
        } else {    
            // Have to do some trickery here due to the fact we're not running in a Spring context,
            // which the HAPI classes assume
            IGenericClient client = context.newRestfulGenericClient(uri);
            FhirClientSearchParamProvider searchParamProvider = new FhirClientSearchParamProvider(client);
            searchParamRegistry.setSearchParamProviderForUnitTest(searchParamProvider);
            setSuperPrivateField(searchParamRegistry, "myFhirContext", context);
            setSuperPrivateField(searchParamRegistry, "myModelConfig", new ModelConfig());
            searchParamRegistry.postConstruct();

            RestFhirRetrieveProvider fhirRetrieveProvider = new RestFhirRetrieveProvider(new SearchParameterResolver(searchParamRegistry), context.newRestfulGenericClient(uri));
            fhirRetrieveProvider.setTerminologyProvider(terminologyProvider);
            fhirRetrieveProvider.setExpandValueSets(true);
            retrieveProvider = fhirRetrieveProvider;        
        }
            
        return new CompositeDataProvider(modelResolver, retrieveProvider);
    }

    private DataProvider getQdmProvider(String version, String uri, TerminologyProvider terminologyProvider) {
        throw new NotImplementedException("QDM data providers are not yet implemented");
    }

    public static void setSuperPrivateField(Object target, String fieldName, Object value){
        try{
            Field privateField = target.getClass().getSuperclass().getDeclaredField(fieldName);
            privateField.setAccessible(true);
            privateField.set(target, value);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

   
}