package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.hapi.fhir.cdshooks.api.ICdsServiceMethod;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;

public class CdsCrServiceMethod extends BaseCdsCrMethod implements ICdsServiceMethod {
    private final CdsServiceJson cdsServiceJson;

    public CdsCrServiceMethod(CdsServiceJson cdsServiceJson, ICdsCrServiceFactory cdsCrServiceFactory) {
        super(cdsCrServiceFactory);
        this.cdsServiceJson = cdsServiceJson;
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
}
