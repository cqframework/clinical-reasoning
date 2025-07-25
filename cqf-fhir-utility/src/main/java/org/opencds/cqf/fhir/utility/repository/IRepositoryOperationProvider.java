package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface IRepositoryOperationProvider {
    <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeOperation(
            IRepository repository, IIdType id, String resourceType, String operationName, IBaseParameters parameters);
}
