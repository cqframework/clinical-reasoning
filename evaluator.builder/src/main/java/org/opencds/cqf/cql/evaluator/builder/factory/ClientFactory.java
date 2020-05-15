package org.opencds.cqf.cql.evaluator.builder.factory;

import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * Create a Client needed to Provide any Data, Artifacts, or Terminology for Evaluation *As of now must be a HAPI Client*
 */
public interface ClientFactory {
    /**
     * create a HAPI Client using a URL
     * @param url url to Client
     * @return HAPI Generic Client
     */
    public IGenericClient create(String url);
}