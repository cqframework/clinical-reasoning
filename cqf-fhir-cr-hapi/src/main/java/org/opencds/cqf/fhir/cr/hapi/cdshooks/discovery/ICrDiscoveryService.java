package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;

public interface ICrDiscoveryService {
    CdsServiceJson resolveService();
}
