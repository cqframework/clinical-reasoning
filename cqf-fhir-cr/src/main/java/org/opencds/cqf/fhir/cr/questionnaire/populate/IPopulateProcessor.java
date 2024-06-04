package org.opencds.cqf.fhir.cr.questionnaire.populate;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IPopulateProcessor {
    // <R extends IBaseResource> R prePopulate(PopulateRequest request);

    IBaseResource populate(PopulateRequest request);

    IBaseResource processResponse(PopulateRequest request, List<IBaseBackboneElement> items);
}
