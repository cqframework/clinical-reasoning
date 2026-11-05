package org.opencds.cqf.fhir.utility.model

import ca.uhn.fhir.context.FhirVersionEnum
import java.util.EnumMap
import org.opencds.cqf.cql.engine.fhir.model.*

object FhirModelResolverCache {
    private val cache: MutableMap<FhirVersionEnum?, FhirModelResolver<*, *, *, *, *, *, *, *>?> =
        EnumMap<FhirVersionEnum?, FhirModelResolver<*, *, *, *, *, *, *, *>?>(
            FhirVersionEnum::class.java
        )

    @JvmStatic
    fun resolverForVersion(
        fhirVersionEnum: FhirVersionEnum
    ): FhirModelResolver<*, *, *, *, *, *, *, *> {
        if (!cache.containsKey(fhirVersionEnum)) {
            val resolver =
                when (fhirVersionEnum) {
                    FhirVersionEnum.DSTU2 -> Dstu2FhirModelResolver()
                    FhirVersionEnum.DSTU3 -> Dstu3FhirModelResolver()
                    FhirVersionEnum.R4 -> R4FhirModelResolver()
                    FhirVersionEnum.R5 -> R5FhirModelResolver()
                    else -> throw IllegalArgumentException("unknown or unsupported FHIR version")
                }

            cache[fhirVersionEnum] = resolver
        }

        return cache[fhirVersionEnum]!!
    }
}
