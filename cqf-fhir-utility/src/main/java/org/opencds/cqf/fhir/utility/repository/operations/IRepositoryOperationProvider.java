package org.opencds.cqf.fhir.utility.repository.operations;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;

public interface IRepositoryOperationProvider {
    <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeOperation(
            Repository repository, IIdType id, String resourceType, String operationName, IBaseParameters parameters);
}
