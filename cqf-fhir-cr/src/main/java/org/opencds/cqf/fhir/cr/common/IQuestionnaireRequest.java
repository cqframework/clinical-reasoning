package org.opencds.cqf.fhir.cr.common;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IQuestionnaireRequest extends IOperationRequest {
    IBaseResource getQuestionnaire();
    
    default List<IBaseBackboneElement> getItems(IBase base) {
        return resolvePathList(base, "item", IBaseBackboneElement.class);
    }

    default Boolean hasItems(IBase base) {
        return !getItems(base).isEmpty();
    }

    default String getItemLinkId(IBaseBackboneElement item) {
        return resolvePathString(item, "linkId");
    }
}
