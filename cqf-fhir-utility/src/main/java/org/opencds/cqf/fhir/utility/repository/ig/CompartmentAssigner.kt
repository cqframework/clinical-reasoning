package org.opencds.cqf.fhir.utility.repository.ig

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.RuntimeSearchParam
import ca.uhn.fhir.model.api.IQueryParameterType
import com.google.common.collect.Multimap
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Ids.ensureIdType
import org.opencds.cqf.fhir.utility.Ids.newId
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentIsolation

/**
 * Determines the compartment assignment for a resource based on repository conventions and resource
 * references.
 */
class CompartmentAssigner(
    private val fhirContext: FhirContext,
    private val compartmentMode: CompartmentMode,
    private val compartmentIsolation: CompartmentIsolation,
) {

    fun fromHeaders(headers: MutableMap<String?, String?>?): CompartmentAssignment {
        if (this.compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.none()
        }

        if (headers == null) {
            return CompartmentAssignment.none()
        }

        val headerValue = headers[IgRepository.FHIR_COMPARTMENT_HEADER]
        if (headerValue.isNullOrBlank()) {
            return CompartmentAssignment.none()
        }

        val segments = headerValue.split("/")
        require(segments.size == 2) {
            "Invalid compartment header value. Expected format: <CompartmentType>/<CompartmentId>"
        }

        val compartmentType = segments[0].trim { it <= ' ' }
        val compartmentId = segments[1].trim { it <= ' ' }
        return CompartmentAssignment.of(compartmentType, compartmentId)
    }

    /**
     * Determines the compartment assignment for the supplied resource based on repository
     * conventions. Possible outcomes:
     * - NONE: No compartment assignment (repository is not compartmentalized)
     * - SHARED: Resource is shared across all compartments
     * - Specific compartment: Resource is assigned to a specific compartment (e.g. Patient/123)
     * - UNKNOWN: Resource belongs to the current compartment type, but the specific compartment
     *   cannot be determined
     *
     * @return The compartment assignment for the resource
     */
    fun fromResource(resource: IBaseResource): CompartmentAssignment {
        requireNotNull(resource.idElement.idPart) { "resource id cannot be null" }

        val resourceType = resource.fhirType()
        val assignment = typeBasedAssignment(resourceType)
        if (!assignment.isUnknown) {
            return assignment
        }
        // If the resource is of the compartment type, assign it to its own compartment.
        if (compartmentMode.type().equals(resourceType, ignoreCase = true)) {
            return CompartmentAssignment.of(compartmentMode.type(), resource.idElement.idPart)
        }

        return resolveFrom(resource)
    }

    /**
     * Determines the compartment assignment for the supplied resource type and id Possible
     * outcomes:
     * - NONE: No compartment assignment (repository is not compartmentalized)
     * - SHARED: Resource is shared across all compartments
     * - Specific compartment: Resource is assigned to a specific compartment (e.g. Patient/123)
     * - UNKNOWN: Resource belongs to the current compartment type, but the specific compartment
     *   cannot be determined
     *
     * @return The compartment assignment for the resource
     */
    fun fromId(resourceType: String, id: IIdType): CompartmentAssignment {

        val assignment = typeBasedAssignment(resourceType)
        if (!assignment.isUnknown) {
            return assignment
        }

        // If the resource is of the compartment type, assign it to its own compartment.
        if (compartmentMode.type().equals(resourceType, ignoreCase = true)) {
            return CompartmentAssignment.of(compartmentMode.type(), id.idPart)
        }

        return CompartmentAssignment.unknown(compartmentMode.type())
    }

    private fun resolveFrom(resource: IBaseResource): CompartmentAssignment {
        val resourceType = resource.fhirType()
        val searchParams = compartmentMode.compartmentSearchParams(fhirContext, resourceType)
        if (searchParams!!.isEmpty()) {
            return CompartmentAssignment.unknown(this.compartmentMode.type())
        }

        val terser = fhirContext.newTerser()
        val compartmentType = compartmentMode.type()

        val orderedParams = orderCompartmentParams(searchParams)

        // Here we're looking at all the attributes on a FHIR resource that
        // potentially provide compartment membership, by matching those up
        // to search paramters that provide compartment membership. We're
        // checking all those paths on the research in priority order
        // (i.e. the "patient" search parameter is more specific than "subject"
        // so search attributes that "patient" indexes on first) looking for
        // references to our current compartment type. The first one we find
        // is the compartment assignment.
        for (param in orderedParams) {
            for (originalPath in param!!.pathsSplit) {
                val path = sanitizeSearchPath(originalPath)
                if (path.isNullOrBlank()) {
                    continue
                }

                val values = terser.getValues(resource, path)
                val ids = referencedIds(values).filter { i -> compartmentType == i.resourceType }

                if (ids.isNotEmpty()) {
                    val id = ids[0]
                    return CompartmentAssignment.of(compartmentType, id.idPart)
                }
            }
        }

        return CompartmentAssignment.shared()
    }

    private fun referencedIds(values: MutableList<IBase?>): MutableList<IIdType> {
        val ids = mutableListOf<IIdType>()
        for (value in values) {
            val reference = extractReference(value)
            if (reference != null && reference.hasResourceType() && reference.hasIdPart()) {
                ids.add(reference)
            }
        }
        return ids
    }

    /**
     * Determines the compartment assignments based on search parameters. If there are multiple
     * possible compartment assignments, only the first is returned. Possible outcomes:
     * - NONE: No compartment assignment (repository is not compartmentalized)
     * - SHARED: Resource is shared across all compartments
     * - Specific compartment: Resource is assigned to a specific compartment (e.g. Patient/123)
     * - UNKNOWN: Resource belongs to the current compartment type, but the specific compartment
     *   cannot be determined
     *
     * @return The compartment assignment for the search
     */
    fun fromSearchParameters(
        resourceType: String,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
    ): CompartmentAssignment {

        var assignment = typeBasedAssignment(resourceType)
        if (!assignment.isUnknown) {
            return assignment
        }

        // Try to assign based on id search parameter
        if (compartmentMode.type().equals(resourceType, ignoreCase = true)) {
            val ids =
                searchParameters!!
                    .get("_id")
                    .filterNotNull()
                    .flatten()
                    .map { obj -> obj!!.valueAsQueryToken }
                    .toList()

            // If no _id or multiple _id parameters, cannot determine assignment
            if (ids.size != 1) {
                return CompartmentAssignment.unknown(compartmentMode.type())
            }

            val ensured = ensureIdType(ids[0]!!, resourceType)
            val id = newId<IIdType>(fhirContext, ensured)
            return CompartmentAssignment.of(compartmentMode.type(), id.idPart)
        }

        val membershipParams =
            orderCompartmentParams(
                compartmentMode.compartmentSearchParams(fhirContext, resourceType)
            )

        for (param in membershipParams) {
            val values = collectQueryParameters(searchParameters, param!!.name)
            if (values.isEmpty()) {
                continue
            }

            // Certain search parameters that target only one type can be abbreviated
            // if they only target one type:
            // Observation?patient=123
            //
            // Others are ambiguous if unspecified:
            // Observation?subject=ABC (Bad! A Patient or Device?)
            // Observation?subject=Patient/123 (Good! We can work with this)
            val target =
                if (param.hasTargets() && param.targets.size == 1) param.targets.iterator().next()
                else null
            assignment = resolveFromQuery(target, values)

            // We found and assignment, bail out.
            if (!assignment.isUnknown) {
                return assignment
            }
        }

        // Did our best, couldn't determine the assignment.
        return CompartmentAssignment.unknown(compartmentMode.type())
    }

    private fun typeBasedAssignment(resourceType: String?): CompartmentAssignment {
        if (compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.none()
        }

        val resourceCategory = ResourceCategory.forType(resourceType)
        if (resourceCategory != ResourceCategory.DATA) {
            return if (compartmentIsolation == CompartmentIsolation.FULL)
                CompartmentAssignment.unknown(this.compartmentMode.type())
            else CompartmentAssignment.shared()
        }

        if (!this.compartmentMode.resourceBelongsToCompartment(fhirContext, resourceType)) {
            return if (compartmentIsolation == CompartmentIsolation.FULL)
                CompartmentAssignment.unknown(this.compartmentMode.type())
            else CompartmentAssignment.shared()
        }

        return CompartmentAssignment.unknown(this.compartmentMode.type())
    }

    private fun sanitizeSearchPath(path: String?): String? {
        if (path == null) {
            return null
        }

        var sanitized: String? = path
        val whereIndex = sanitized.indexOf(".where(")
        if (whereIndex >= 0) {
            sanitized = sanitized.substring(0, whereIndex)
        }

        // The terser expects paths relative to the resource and does not understand resolve()
        return sanitized.replace(".resolve()", "")
    }

    private fun extractReference(value: Any?): IIdType? {
        if (value is IIdType) {
            return value
        }

        if (value is IPrimitiveType<*>) {
            val asString = value.valueAsString
            if (asString.isNullOrBlank()) {
                return null
            }

            return newId(fhirContext, asString)
        }

        if (value is IBaseReference) {
            return value.referenceElement
        }

        return null
    }

    private fun orderCompartmentParams(
        params: MutableSet<RuntimeSearchParam?>?
    ): MutableList<RuntimeSearchParam?> {
        return params!!
            .sortedWith(
                Comparator.comparingInt<RuntimeSearchParam?> { param ->
                        priorityIndex(param!!.name)
                    }
                    .thenComparing { obj -> obj!!.name }
            )
            .toMutableList()
    }

    private fun priorityIndex(name: String?): Int {
        if (name == null) {
            return COMPARTMENT_REFERENCE_PRIORITY.size
        }
        val normalized = name.lowercase()
        val index = COMPARTMENT_REFERENCE_PRIORITY.indexOf(normalized)
        return if (index >= 0) index else COMPARTMENT_REFERENCE_PRIORITY.size
    }

    private fun collectQueryParameters(
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        expectedName: String?,
    ): MutableList<IQueryParameterType?> {
        val matches = mutableListOf<IQueryParameterType?>()
        if (searchParameters == null || expectedName == null) {
            return matches
        }

        val normalized = expectedName.lowercase()
        val searchParamKeys =
            searchParameters.keySet().filterNotNull().map { obj -> obj.lowercase() }.toList()

        for (key in searchParamKeys) {
            if (!key.startsWith(normalized)) {
                continue
            }

            val searchGroups = searchParameters.get(key)
            for (group in searchGroups) {
                matches.addAll(group!!)
            }
        }

        return matches
    }

    private fun resolveFromQuery(
        targetIfUnspecified: String?,
        values: MutableList<IQueryParameterType?>,
    ): CompartmentAssignment {
        val compartmentType = compartmentMode.type()
        for (queryValue in values) {
            val token = queryValue!!.valueAsQueryToken
            val id = idFromToken(targetIfUnspecified, token)
            if (id != null && compartmentType.equals(id.resourceType, ignoreCase = true)) {
                return CompartmentAssignment.of(compartmentType, id.idPart)
            }
        }

        return CompartmentAssignment.unknown(this.compartmentMode.type())
    }

    private fun idFromToken(target: String?, token: String?): IIdType? {
        if (token.isNullOrBlank()) {
            return null
        }

        // If no target is specified, assume the token is of the target compartment type
        if (target.isNullOrBlank()) {
            return newId(fhirContext, token)
        } else {
            val ensured = ensureIdType(token, target)
            return newId(fhirContext, ensured)
        }
    }

    companion object {
        // Search parameters that likely reference compartments,
        // in order of priority to use to determine compartment membership
        private val COMPARTMENT_REFERENCE_PRIORITY =
            mutableListOf<String?>(
                "patient",
                "subject",
                "beneficiary",
                "member",
                "individual",
                "encounter",
                "episodeofcare",
            )
    }
}
