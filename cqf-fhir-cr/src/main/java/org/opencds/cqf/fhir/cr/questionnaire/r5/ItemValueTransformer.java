package org.opencds.cqf.fhir.cr.questionnaire.r5;

import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.DataType;

public class ItemValueTransformer {
    private ItemValueTransformer() {}

    public static DataType transformValue(DataType value) {
        if (value instanceof CodeableConcept) {
            return ((CodeableConcept) value).getCoding().get(0);
        }

        return value;
    }
}
