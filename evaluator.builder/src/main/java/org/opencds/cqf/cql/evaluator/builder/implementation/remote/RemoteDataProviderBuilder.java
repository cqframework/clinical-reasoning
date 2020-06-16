package org.opencds.cqf.cql.evaluator.builder.implementation.remote;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.apache.commons.lang3.NotImplementedException;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.engine.fhir.model.Dstu2FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.retrieve.RestFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

import org.apache.commons.lang3.tuple.Pair;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class RemoteDataProviderBuilder {
    private TerminologyProvider terminologyProvider;

    public RemoteDataProviderBuilder(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
    }

    public Map<String, DataProvider> build(List<URL> urlList, ClientFactory clientFactory)
            throws IOException, InterruptedException, URISyntaxException {
        if (clientFactory == null) {
            throw new IllegalArgumentException(String.format("Needed to access remote url %s and ClientFactory was null."));
        }
        Map<String, DataProvider> dataProviderMap = new HashMap<String, DataProvider>();
        for (URL url : urlList) {
            IGenericClient client = clientFactory.create(url);
            Pair<String, DataProvider> modelProviderPair = forFhirProviderClients(client);
            dataProviderMap.putIfAbsent(modelProviderPair.getLeft(), modelProviderPair.getRight());
        }
        return dataProviderMap;
    }

    @SuppressWarnings("rawtypes")
    private Pair<String, DataProvider> forFhirProviderClients(IGenericClient client) {
        FhirModelResolver modelResolver;
        RetrieveProvider retrieveProvider;
        FhirContext fhirContext = client.getFhirContext();
        FhirVersionEnum versionEnum = fhirContext.getVersion().getVersion();
        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no Remote Data Provider implementation for anything older than DSTU2 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            modelResolver = new Dstu2FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(client, fhirContext);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            modelResolver = new Dstu3FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(client, fhirContext);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            modelResolver = new R4FhirModelResolver();
            retrieveProvider = buildRetrieveProvider(client, fhirContext);        
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no Remote Data Provider implementation for anything newer than or equal to R5 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum.");
        }
        return Pair.of("http://hl7.org/fhir", new CompositeDataProvider(modelResolver, retrieveProvider));
    }

    private RetrieveProvider buildRetrieveProvider(IGenericClient client, FhirContext fhirContext) {
        RestFhirRetrieveProvider fhirRetrieveProvider = new RestFhirRetrieveProvider(new SearchParameterResolver(fhirContext), client);
        fhirRetrieveProvider.setTerminologyProvider(terminologyProvider);
        fhirRetrieveProvider.setExpandValueSets(true);
        return fhirRetrieveProvider;
    }
}