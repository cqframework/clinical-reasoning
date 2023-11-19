package org.opencds.cqf.fhir.cr.questionnaire.r5.processor;

import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Questionnaire;

public class GenerateProcessor {
    public Questionnaire generate(String id) {
        var questionnaire = new Questionnaire();
        questionnaire.setId(new IdType("Questionnaire", id));
        return questionnaire;
    }
}
