package org.opencds.cqf.fhir.cr.questionnaire.populate;

import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IPopulateProcessor {
    IBaseResource populate(PopulateRequest request);
}
