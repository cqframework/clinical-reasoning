package org.opencds.cqf.cql.evaluator.factory;

import ca.uhn.fhir.rest.client.api.IGenericClient;


public interface ClientFactory {
    public IGenericClient create(String url);
}