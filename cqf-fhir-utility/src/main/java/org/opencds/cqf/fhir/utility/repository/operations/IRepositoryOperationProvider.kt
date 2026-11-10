package org.opencds.cqf.fhir.utility.repository.operations

import ca.uhn.fhir.repository.IRepository
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.instance.model.api.IPrimitiveType

interface IRepositoryOperationProvider {
    fun <C : IPrimitiveType<String?>, R : IBaseResource> invokeOperation(
        repository: IRepository,
        id: IIdType?,
        resourceType: String?,
        operationName: String?,
        parameters: IBaseParameters?,
    ): R?
}
