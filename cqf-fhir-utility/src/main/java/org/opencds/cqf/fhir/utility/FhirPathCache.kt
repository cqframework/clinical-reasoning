package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.fhirpath.IFhirPath
import java.util.concurrent.ConcurrentHashMap

object FhirPathCache {
    private val CACHE: MutableMap<FhirVersionEnum, IFhirPath> = ConcurrentHashMap()

    @JvmStatic
    fun cachedForContext(fhirContext: FhirContext): IFhirPath {
        return CACHE.computeIfAbsent(fhirContext.version.version) { x -> fhirContext.newFhirPath() }
    }

    @JvmStatic
    fun cachedForVersion(fhirVersionEnum: FhirVersionEnum): IFhirPath {
        return CACHE.computeIfAbsent(fhirVersionEnum) { x ->
            FhirContext.forCached(x).newFhirPath()
        }
    }
}
