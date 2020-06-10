package org.opencds.cqf.cql.evaluator.builder.implementation.remote;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.engine.fhir.exception.UnknownElement;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class RemoteTerminologyProviderBuilder {

    public TerminologyProvider build(URL url, Map<String, Pair<String, String>> models, ClientFactory clientFactory)
            throws IOException, InterruptedException, URISyntaxException {
        if (clientFactory == null) {
            throw new IllegalArgumentException(String.format("Needed to access remote url %s and ClientFactory was null."));
        }
        TerminologyProvider terminologyProvider = null;
        IGenericClient client = clientFactory.create(url);
        //could compare this to library models in order to validate use
        FhirVersionEnum versionEnum = client.getFhirContext().getVersion().getVersion();
        if (versionEnum.isOlderThan(FhirVersionEnum.DSTU2)) {
            throw new NotImplementedException("Sorry there is no implementation for anything older than DSTU3 as of now.");
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU2) && versionEnum.isOlderThan(FhirVersionEnum.DSTU3)) {
            throw new NotImplementedException("Sorry there is no implementation for anything older than DSTU3 as of now.");      
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.DSTU3) && versionEnum.isOlderThan(FhirVersionEnum.R4)) {
            terminologyProvider = new Dstu3FhirTerminologyProvider(client); 
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R4) && versionEnum.isOlderThan(FhirVersionEnum.R5)) {
            terminologyProvider = new R4FhirTerminologyProvider(client);
        }
        else if (versionEnum.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new NotImplementedException("Sorry there is no implementation for anything newer than or equal to R5 as of now.");
        }
        else {
            throw new UnknownElement("Unknown Fhir Version Enum");
        }
        return terminologyProvider;
    }
    
}