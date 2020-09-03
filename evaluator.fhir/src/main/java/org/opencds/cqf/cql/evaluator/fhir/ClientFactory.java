package org.opencds.cqf.cql.evaluator.fhir;

import java.util.List;

import ca.uhn.fhir.rest.client.api.IGenericClient;


/**
 * This class creates a FHIR Rest API IGenericClient for a given fhirContext an url.
 */
public interface ClientFactory {
    public IGenericClient create(String url, List<String> headers); 
}
