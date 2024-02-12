package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;

public class ItemValueTransformer {
    private ItemValueTransformer() {}

    @SuppressWarnings("rawtypes")
    public static org.hl7.fhir.dstu3.model.Type transformValueToItem(org.hl7.fhir.dstu3.model.Type value) {
        if (value instanceof org.hl7.fhir.dstu3.model.CodeableConcept) {
            return ((org.hl7.fhir.dstu3.model.CodeableConcept) value)
                    .getCoding()
                    .get(0);
        }
        if (value instanceof org.hl7.fhir.dstu3.model.Enumeration) {
            return new org.hl7.fhir.dstu3.model.StringType(
                    ((org.hl7.fhir.dstu3.model.Enumeration) value).asStringValue());
        }
        return value;
    }

    @SuppressWarnings("rawtypes")
    public static org.hl7.fhir.r4.model.Type transformValueToItem(org.hl7.fhir.r4.model.Type value) {
        if (value instanceof org.hl7.fhir.r4.model.CodeableConcept) {
            return ((org.hl7.fhir.r4.model.CodeableConcept) value).getCoding().get(0);
        }
        if (value instanceof org.hl7.fhir.r4.model.Enumeration) {
            return new org.hl7.fhir.r4.model.StringType(((org.hl7.fhir.r4.model.Enumeration) value).getCode());
        }
        return value;
    }

    @SuppressWarnings("rawtypes")
    public static org.hl7.fhir.r5.model.DataType transformValueToItem(org.hl7.fhir.r5.model.DataType value) {
        if (value instanceof org.hl7.fhir.r5.model.CodeableConcept) {
            return ((org.hl7.fhir.r5.model.CodeableConcept) value).getCoding().get(0);
        }
        if (value instanceof org.hl7.fhir.r5.model.Enumeration) {
            return new org.hl7.fhir.r5.model.StringType(((org.hl7.fhir.r5.model.Enumeration) value).getCode());
        }
        return value;
    }

    public static IBase transformValueToResource(FhirVersionEnum fhirVersion, IBase value) {
        switch (fhirVersion) {
            case DSTU3:
                return transformValueToResource((org.hl7.fhir.dstu3.model.Type) value);
            case R4:
                return transformValueToResource((org.hl7.fhir.r4.model.Type) value);
            case R5:
                return transformValueToResource((org.hl7.fhir.r5.model.DataType) value);

            default:
                return null;
        }
    }

    public static org.hl7.fhir.dstu3.model.Type transformValueToResource(org.hl7.fhir.dstu3.model.Type value) {
        if (value instanceof org.hl7.fhir.dstu3.model.Coding) {
            return new org.hl7.fhir.dstu3.model.CodeableConcept().addCoding((org.hl7.fhir.dstu3.model.Coding) value);
        }
        return value;
    }

    public static org.hl7.fhir.r4.model.Type transformValueToResource(org.hl7.fhir.r4.model.Type value) {
        if (value instanceof org.hl7.fhir.r4.model.Coding) {
            return new org.hl7.fhir.r4.model.CodeableConcept().addCoding((org.hl7.fhir.r4.model.Coding) value);
        }
        return value;
    }

    public static org.hl7.fhir.r5.model.DataType transformValueToResource(org.hl7.fhir.r5.model.DataType value) {
        if (value instanceof org.hl7.fhir.r5.model.Coding) {
            return new org.hl7.fhir.r5.model.CodeableConcept().addCoding((org.hl7.fhir.r5.model.Coding) value);
        }
        return value;
    }
}
