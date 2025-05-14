package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

public abstract class BaseCdsCrTest {
    protected FhirContext fhirContext;
    protected IRepository repository;
    protected RestfulServer restfulServer;
    protected IAdapterFactory adapterFactory;

    protected final CdsCrSettings cdsSettings = new CdsCrSettings();
    protected ObjectMapper objectMapper = new ObjectMapper();

    protected IRepository getRepository() {
        return new InMemoryFhirRepository(fhirContext);
    }

    protected RestfulServer getRestfulServer() {
        return new RestfulServer(fhirContext);
    }

    protected IAdapterFactory getAdapterFactory() {
        return IAdapterFactory.forFhirContext(fhirContext);
    }
}
