package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import org.hl7.fhir.instance.model.api.IBaseResource;

class ProcessorHelper {
    protected <T extends IBaseResource>String getExtractId(T questionnaireResponse) {
        return "extract-" + (questionnaireResponse).getIdElement().getIdPart();
    }
}
