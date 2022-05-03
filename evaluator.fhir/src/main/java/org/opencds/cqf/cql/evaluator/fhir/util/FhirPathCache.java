package org.opencds.cqf.cql.evaluator.fhir.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;

public class FhirPathCache {

    private final static Map<FhirVersionEnum, IFhirPath> fhirPathCache = new ConcurrentHashMap<>();

    public static IFhirPath cachedForContext(FhirContext fhirContext) {
        return fhirPathCache.computeIfAbsent(fhirContext.getVersion().getVersion(), x -> fhirContext.newFhirPath());
    }

    public static IFhirPath cachedForVersion(FhirVersionEnum fhirVersionEnum) {
        return fhirPathCache.computeIfAbsent(fhirVersionEnum, x -> x.newContext().newFhirPath());
    }
    
}
