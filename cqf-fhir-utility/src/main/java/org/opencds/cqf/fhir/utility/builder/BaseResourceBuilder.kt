package org.opencds.cqf.fhir.utility.builder

import ca.uhn.fhir.context.FhirVersionEnum
import java.util.UUID
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Resources

abstract class BaseResourceBuilder<SELF, T : IBaseResource>
protected constructor(private val resourceClass: Class<T>) {

    private var profile: MutableList<String?>? = null

    protected var id = UUID.randomUUID().toString()
        private set

    protected var identifier: Pair<String?, String?>? =
        Pair<String?, String?>(
            DEFAULT_IDENTIFIER_SYSTEM,
            DEFAULT_IDENTIFIER_VALUE_PREFIX + UUID.randomUUID().toString(),
        )
        private set

    protected constructor(resourceClass: Class<T>, id: String) : this(resourceClass) {
        this.id = id
    }

    protected open fun self(): SELF {
        @Suppress("UNCHECKED_CAST")
        return this as SELF
    }

    private fun addProfile(profile: String?) {
        if (this.profile == null) {
            this.profile = ArrayList<String?>()
        }

        this.profile!!.add(profile)
    }

    protected val profiles: MutableList<String?>
        get() {
            if (profile == null) {
                return mutableListOf()
            }

            return profile!!
        }

    fun withId(id: String): SELF {
        this.id = id

        return self()
    }

    fun withProfile(profile: String): SELF {

        addProfile(profile)

        return self()
    }

    fun withIdentifier(identifier: Pair<String?, String?>?): SELF {
        this.identifier = identifier

        return self()
    }

    open fun build(): T {
        val resource = Resources.newResource<T, IIdType>(resourceClass, id)

        when (resource.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> initializeDstu3(resource)
            FhirVersionEnum.R4 -> initializeR4(resource)
            FhirVersionEnum.R5 -> initializeR5(resource)
            else ->
                throw IllegalArgumentException(
                    "ResourceBuilder.initializeResource does not support FHIR version ${resource.structureFhirVersionEnum.fhirVersionString}"
                )
        }

        return resource
    }

    private fun addProfiles(resource: T?) {
        this.profiles.forEach { p -> resource!!.meta.addProfile(p) }
    }

    protected open fun initializeDstu3(resource: T?) {
        addProfiles(resource)
    }

    protected open fun initializeR4(resource: T?) {
        addProfiles(resource)
    }

    protected open fun initializeR5(resource: T?) {
        addProfiles(resource)
    }

    companion object {
        const val DEFAULT_IDENTIFIER_SYSTEM: String = "urn:ietf:rfc:3986"
        const val DEFAULT_IDENTIFIER_VALUE_PREFIX: String = "urn:uuid:"

        @JvmStatic
        fun ensurePatientReference(patientId: String?): String? {
            if (patientId.isNullOrEmpty() || patientId.startsWith("Patient/")) {
                return patientId
            }
            return "Patient/$patientId"
        }

        @JvmStatic
        fun ensureOrganizationReference(organizationId: String?): String? {
            if (organizationId.isNullOrEmpty() || organizationId.startsWith("Organization/")) {
                return organizationId
            }
            return "Organization/$organizationId"
        }
    }
}
