package org.opencds.cqf.fhir.utility.behavior

import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Ids

interface IdCreator : FhirContextUser {
    fun <T : IIdType> newId(resourceName: String, resourceId: String): T {
        return Ids.newId(fhirContext, resourceName, resourceId)
    }

    fun <T : IIdType> newId(resourceId: String): T {
        return Ids.newId(fhirContext, resourceId)
    }
}
