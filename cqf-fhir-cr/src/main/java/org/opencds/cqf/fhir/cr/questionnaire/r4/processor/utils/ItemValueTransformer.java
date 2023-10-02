package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Type;

public class ItemValueTransformer {
    private ItemValueTransformer() {}

    public static Type transformValue(Type value) {
        if (value instanceof CodeableConcept) {
            return ((CodeableConcept) value).getCoding().get(0);
        }

        return value;
    }
}
