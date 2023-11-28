package org.opencds.cqf.fhir.cr.questionnaire.common;

public class ItemValueTransformer {
    private ItemValueTransformer() {}

    public static org.hl7.fhir.dstu3.model.Type transformValue(org.hl7.fhir.dstu3.model.Type value) {
        if (value instanceof org.hl7.fhir.dstu3.model.CodeableConcept) {
            return ((org.hl7.fhir.dstu3.model.CodeableConcept) value)
                    .getCoding()
                    .get(0);
        }
        return value;
    }

    public static org.hl7.fhir.r4.model.Type transformValue(org.hl7.fhir.r4.model.Type value) {
        if (value instanceof org.hl7.fhir.r4.model.CodeableConcept) {
            return ((org.hl7.fhir.r4.model.CodeableConcept) value).getCoding().get(0);
        }
        return value;
    }

    public static org.hl7.fhir.r5.model.DataType transformValue(org.hl7.fhir.r5.model.DataType value) {
        if (value instanceof org.hl7.fhir.r5.model.CodeableConcept) {
            return ((org.hl7.fhir.r5.model.CodeableConcept) value).getCoding().get(0);
        }
        return value;
    }
}
