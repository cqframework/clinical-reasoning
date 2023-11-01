package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.generate;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;

public class GenerateProcessor {
    public Questionnaire generate(String id) {
        var questionnaire = new Questionnaire();
        questionnaire.setId(new IdType("Questionnaire", id));
        return questionnaire;
    }
}
