package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsConfigService;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsConfigServiceImpl;

public abstract class BaseCdsCrServiceTest extends BaseCdsCrTest {
    protected RequestDetails requestDetails = new SystemRequestDetails();

    protected CdsResponseEncoderService cdsResponseEncoderService;
    protected CdsParametersEncoderService cdsParametersEncoderService;

    protected ICdsConfigService cdsConfigService;

    public CdsResponseEncoderService getCdsResponseEncoderService() {
        if (cdsResponseEncoderService == null) {
            cdsResponseEncoderService = new CdsResponseEncoderService(getRepository(), getAdapterFactory());
        }

        return cdsResponseEncoderService;
    }

    public CdsParametersEncoderService getCdsParametersEncoderService() {
        if (cdsParametersEncoderService == null) {
            cdsParametersEncoderService = new CdsParametersEncoderService(getRepository(), getAdapterFactory());
        }

        return cdsParametersEncoderService;
    }

    protected ICdsConfigService getCdsConfigService() {
        return new CdsConfigServiceImpl(fhirContext, objectMapper, null, getRestfulServer());
    }
}
