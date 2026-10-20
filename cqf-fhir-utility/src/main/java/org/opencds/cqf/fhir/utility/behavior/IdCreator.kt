package org.opencds.cqf.fhir.utility.behavior;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Ids;

public interface IdCreator extends FhirContextUser {

    default <T extends IIdType> T newId(String resourceName, String resourceId) {
        checkNotNull(resourceName);
        checkNotNull(resourceId);

        return Ids.newId(getFhirContext(), resourceName, resourceId);
    }

    default <T extends IIdType> T newId(String resourceId) {
        checkNotNull(resourceId);

        return Ids.newId(getFhirContext(), resourceId);
    }
}
