package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.model.api.IElement;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IIdType;

public interface ICpgRequest extends IQuestionnaireRequest {
    IIdType getEncounterId();

    default boolean hasEncounterId() {
        return getEncounterId() != null && !getEncounterId().isEmpty();
    }

    IIdType getPractitionerId();

    default boolean hasPractitionerId() {
        return getPractitionerId() != null && !getPractitionerId().isEmpty();
    }

    IIdType getOrganizationId();

    default boolean hasOrganizationId() {
        return getOrganizationId() != null && !getOrganizationId().isEmpty();
    }

    IBaseDatatype getUserType();

    IBaseDatatype getUserLanguage();

    IBaseDatatype getUserTaskContext();

    IBaseDatatype getSetting();

    IBaseDatatype getSettingContext();

    default List<IBaseBackboneElement> getDynamicValues(IElement element) {
        return resolvePathList(element, "dynamicValue", IBaseBackboneElement.class);
    }
}
