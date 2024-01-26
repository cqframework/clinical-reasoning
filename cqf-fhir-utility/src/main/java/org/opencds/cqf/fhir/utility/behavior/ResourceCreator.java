package org.opencds.cqf.fhir.utility.behavior;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Ids;

public interface ResourceCreator extends FhirContextUser {
    @SuppressWarnings("unchecked")
    default <T extends IBaseResource, I extends IIdType> T newResource(I id) {
        checkNotNull(id, "id is required");
        checkArgument(id.getResourceType() != null, "id must have a resourceType");

        IBaseResource newResource = this.getFhirContext()
                .getResourceDefinition(id.getResourceType())
                .newInstance();
        newResource.setId(id);
        return (T) newResource;
    }

    default <T extends IBaseResource> T newResource(Class<T> resourceClass, String idPart) {
        checkNotNull(resourceClass);
        checkNotNull(idPart);

        T newResource = newResource(resourceClass);
        IIdType id = Ids.newId(getFhirContext(), newResource.fhirType(), idPart);
        newResource.setId(id);

        return newResource;
    }

    @SuppressWarnings("unchecked")
    default <T extends IBaseResource> T newResource(Class<T> resourceClass) {
        checkNotNull(resourceClass);

        return (T) this.getFhirContext().getResourceDefinition(resourceClass).newInstance();
    }

    @SuppressWarnings("unchecked")
    default <T extends IBaseResource> T newResource(String resourceType) {
        checkNotNull(resourceType);

        return (T) this.getFhirContext().getResourceDefinition(resourceType).newInstance();
    }
}
