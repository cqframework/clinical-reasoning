package org.opencds.cqf.cql.evaluator.builder.factory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * Create a Client needed to Provide any Data, Artifacts, or Terminology for Evaluation *As of now must be a HAPI Client*
 */
public interface ClientFactory {
    /**
     * create a HAPI Client using a URL
     * @param url url to Client
     * @return HAPI Generic Client
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public IGenericClient create(URL url) throws IOException, InterruptedException, URISyntaxException;
}