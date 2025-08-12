package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsServiceMethod;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CdsCrServiceMethod extends BaseCdsCrMethod implements ICdsServiceMethod {
    private final CdsServiceJson cdsServiceJson;
    private final ObjectMapper objectMapper;

    public CdsCrServiceMethod(CdsServiceJson cdsServiceJson, ICdsCrServiceFactory cdsCrServiceFactory) {
        this(new ObjectMapper(), cdsServiceJson, cdsCrServiceFactory);
    }

    public CdsCrServiceMethod(
            ObjectMapper objectMapper, CdsServiceJson cdsServiceJson, ICdsCrServiceFactory cdsCrServiceFactory) {
        super(cdsCrServiceFactory);
        this.cdsServiceJson = cdsServiceJson;
        this.objectMapper = objectMapper;
    }

    @Override
    public CdsServiceJson getCdsServiceJson() {
        return cdsServiceJson;
    }

    @Override
    public boolean isAllowAutoFhirClientPrefetch() {
        // The $apply operation will NOT make FHIR requests for any data it needs.
        return true;
    }

    public Object invoke(IModelJson json, String serviceId) {
        return this.invoke(objectMapper, json, serviceId);
    }
}
