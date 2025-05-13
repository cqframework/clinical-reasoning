package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;

/**
 * In addition to the REST service strings maintained in {@link ProviderConstants}, such as
 * $evaluate-measure, add any constants for new REST services here.
 */
public class CrProviderConstants {
    public static final String CR_OPERATION_EVALUATE_MEASURE_URL = "$evaluate-measure-url";

    private CrProviderConstants() {
        // private constructor
    }
}
