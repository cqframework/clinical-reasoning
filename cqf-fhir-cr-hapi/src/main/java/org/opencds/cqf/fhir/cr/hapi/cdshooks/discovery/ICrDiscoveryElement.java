package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;

public interface ICrDiscoveryElement {
    CdsServiceJson getCdsServiceJson();

    default String getKey(int itemNo) {
        return "item" + Integer.toString(itemNo);
    }
}
