package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.cr.common.IRepositoryFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsConfigService;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsConfigServiceImpl;

public abstract class BaseCdsCrServiceTest extends BaseCdsCrTest {
    protected static final RequestDetails REQUEST_DETAILS = new SystemRequestDetails();

    protected IRepositoryFactory repositoryFactory;
    protected ICdsConfigService cdsConfigService;

    protected ICdsConfigService getCdsConfigService() {
        return new CdsConfigServiceImpl(fhirContext, objectMapper, cdsSettings, null, repositoryFactory, restfulServer);
    }
}
