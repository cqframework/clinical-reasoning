package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Questionnaire;

public class GenerateProcessor {
    public Questionnaire generate(String id) {
        var questionnaire = new Questionnaire();
        questionnaire.setId(new IdType("Questionnaire", id));
        return questionnaire;
    }
}
