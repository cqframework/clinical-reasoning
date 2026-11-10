package org.opencds.cqf.fhir.utility.repository.ig

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.context.RuntimeSearchParam
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Whether or not the files are organized by compartment. This is primarily used for tests to
 * provide isolation between test cases.
 */
enum class CompartmentMode(private val type: String) {
    PATIENT("Patient"),
    ENCOUNTER("Encounter"),
    RELATED_PERSON("RelatedPerson"),
    PRACTITIONER("Practitioner"),
    GROUP("Group"),
    NONE("");

    fun type(): String {
        return type
    }

    fun resourceBelongsToCompartment(fhirContext: FhirContext, resourceType: String?): Boolean {
        return compartmentSearchParams(fhirContext, resourceType)!!.isNotEmpty()
    }

    fun compartmentSearchParams(
        fhirContext: FhirContext,
        resourceType: String?,
    ): MutableSet<RuntimeSearchParam?>? {
        val key = CacheKey(this.type, fhirContext.version.version, resourceType)
        return COMPARTMENT_MEMBERSHIP_CACHE.computeIfAbsent(key) {
            computeSearchParams(fhirContext, resourceType)
        }
    }

    private fun computeSearchParams(
        fhirContext: FhirContext,
        resourceType: String?,
    ): MutableSet<RuntimeSearchParam?> {
        val params: MutableSet<RuntimeSearchParam?>?
        if (this.type == resourceType) {
            params =
                fhirContext
                    .getResourceDefinition(resourceType)
                    .searchParams
                    .filter { param -> param!!.name == "_id" }
                    .toMutableSet()
            check(params.isNotEmpty()) {
                "No _id search parameter found for resource type: $resourceType"
            }
        } else {
            params =
                fhirContext
                    .getResourceDefinition(resourceType)
                    .searchParams
                    .filter { param -> param!!.paramType == RestSearchParameterTypeEnum.REFERENCE }
                    .filter { param ->
                        (param!!.providesMembershipInCompartments != null &&
                            param.providesMembershipInCompartments.contains(this.type)) ||
                            (param.targets != null && param.targets.contains(this.type))
                    }
                    .toMutableSet()
        }

        return if (params.isEmpty()) mutableSetOf() else params.toMutableSet()
    }

    @JvmRecord
    private data class CacheKey(
        val compartmentType: String?,
        val version: FhirVersionEnum?,
        val resourceType: String?,
    )

    companion object {
        private val COMPARTMENT_MEMBERSHIP_CACHE:
            ConcurrentMap<CacheKey?, MutableSet<RuntimeSearchParam?>?> =
            ConcurrentHashMap<CacheKey?, MutableSet<RuntimeSearchParam?>?>()

        fun fromType(type: String?): CompartmentMode {
            for (mode in entries) {
                if (mode.type.equals(type, ignoreCase = true)) {
                    return mode
                }
            }
            return NONE
        }
    }
}
