package org.opencds.cqf.fhir.cr.questionnaireresponse.common;

import org.hl7.fhir.instance.model.api.IBaseResource;

public class ProcessorHelper {
    public <T extends IBaseResource>String getExtractId(T questionnaireResponse) {
        return "extract-" + (questionnaireResponse).getIdElement().getIdPart();
    }

    public IBaseResource newValue(String type) {
        try {
            return (IBaseResource) Class.forName("org.hl7.fhir.r4.model." + type)
                .getConstructor()
                .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
