package org.opencds.cqf.fhir.cr.questionnaire.generate;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Ids;

public class GenerateProcessor {
    protected final FhirVersionEnum fhirVersion;

    public GenerateProcessor(FhirVersionEnum fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public IBaseResource generate(String id) {
        var questionnaire = generateQuestionnaire();
        var newId = Ids.newId(fhirVersion, Ids.ensureIdType(id, "Questionnaire"));
        questionnaire.setId(newId);
        return questionnaire;
    }

    protected IBaseResource generateQuestionnaire() {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Questionnaire();
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire();
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire();

            default:
                return null;
        }
    }
}
