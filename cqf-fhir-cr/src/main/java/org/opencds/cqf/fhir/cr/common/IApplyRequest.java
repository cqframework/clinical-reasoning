package org.opencds.cqf.fhir.cr.common;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.model.api.IElement;

public interface IApplyRequest extends IQuestionnaireRequest {
    IIdType getEncounterId();

    default Boolean hasEncounterId() {
        return getEncounterId() != null && !getEncounterId().isEmpty();
    }

    IIdType getPractitionerId();

    default Boolean hasPractitionerId() {
        return getPractitionerId() != null && !getPractitionerId().isEmpty();
    }

    IIdType getOrganizationId();

    default Boolean hasOrganizationId() {
        return getOrganizationId() != null && !getOrganizationId().isEmpty();
    }

    IBaseDatatype getUserType();

    IBaseDatatype getUserLanguage();

    IBaseDatatype getUserTaskContext();

    IBaseDatatype getSetting();

    IBaseDatatype getSettingContext();

    Boolean getUseServerData();

    default List<IBaseBackboneElement> getDynamicValues(IElement element) {
        return resolvePathList(element, "dynamicValue", IBaseBackboneElement.class);
    }
}
