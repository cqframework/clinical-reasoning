package org.opencds.cqf.fhir.utility.behavior

import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Ids

interface ResourceCreator : FhirContextUser {
    fun <T : IBaseResource, I : IIdType> newResource(id: I): T {
        requireNotNull(id.resourceType) { "id must have a resourceType" }

        val newResource = this.fhirContext.getResourceDefinition(id.resourceType).newInstance()
        newResource.setId(id)
        return newResource as T
    }

    fun <T : IBaseResource> newResource(resourceClass: Class<T>, idPart: String): T {
        val newResource = newResource(resourceClass)
        val id = Ids.newId<IIdType>(fhirContext, newResource.fhirType(), idPart)
        newResource.setId(id)

        return newResource
    }

    fun <T : IBaseResource> newResource(resourceClass: Class<T>): T {
        return this.fhirContext.getResourceDefinition(resourceClass).newInstance() as T
    }

    fun <T : IBaseResource> newResource(resourceType: String): T {
        return this.fhirContext.getResourceDefinition(resourceType).newInstance() as T
    }
}
