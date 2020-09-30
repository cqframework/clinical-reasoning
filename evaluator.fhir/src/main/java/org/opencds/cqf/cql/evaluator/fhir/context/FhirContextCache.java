package org.opencds.cqf.cql.evaluator.fhir.context;

import java.util.HashMap;
import java.util.Map;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class FhirContextCache {
    private Map<FhirVersionEnum, FhirContext> fhirContextByVersion = new HashMap<>();

    public FhirContext getContext(FhirVersionEnum fhirVersionEnum) {
        if (fhirVersionEnum == null) {
            throw new IllegalArgumentException("fhirVersionEnum must not be null");
        }

        if (!fhirContextByVersion.containsKey(fhirVersionEnum)) {
            fhirContextByVersion.put(fhirVersionEnum, fhirVersionEnum.newContext());
        }

        return fhirContextByVersion.get(fhirVersionEnum);
    }
}