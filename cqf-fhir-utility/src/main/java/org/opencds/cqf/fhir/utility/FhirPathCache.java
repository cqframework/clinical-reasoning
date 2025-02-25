package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FhirPathCache {

    private FhirPathCache() {}

    private static final Map<FhirVersionEnum, IFhirPath> CACHE = new ConcurrentHashMap<>();

    public static IFhirPath cachedForContext(FhirContext fhirContext) {
        return CACHE.computeIfAbsent(fhirContext.getVersion().getVersion(), x -> fhirContext.newFhirPath());
    }

    public static IFhirPath cachedForVersion(FhirVersionEnum fhirVersionEnum) {
        return CACHE.computeIfAbsent(fhirVersionEnum, x -> x.newContext().newFhirPath());
        // when we upgrade to HAPI FHIR 5.0.0, we can use the following line
        //        return CACHE.computeIfAbsent(fhirVersionEnum, fhirVersion -> FhirContext.forVersion(fhirVersion)
        //                .newFhirPath());
    }
}
