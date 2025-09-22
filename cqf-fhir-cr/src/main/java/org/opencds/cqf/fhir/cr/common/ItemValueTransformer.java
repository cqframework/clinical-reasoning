package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;

public class ItemValueTransformer {
    private ItemValueTransformer() {}

    public static IBaseDatatype transformValueToItem(FhirVersionEnum fhirVersion, IBase value) {
        return switch (fhirVersion) {
            case DSTU3 -> transformValueToItem((org.hl7.fhir.dstu3.model.Type) value);
            case R4 -> transformValueToItem((org.hl7.fhir.r4.model.Type) value);
            case R5 -> transformValueToItem((org.hl7.fhir.r5.model.DataType) value);
            default -> null;
        };
    }

    public static org.hl7.fhir.dstu3.model.Type transformValueToItem(org.hl7.fhir.dstu3.model.Type value) {
        if (value instanceof org.hl7.fhir.dstu3.model.CodeableConcept codeType) {
            return codeType.getCoding().get(0);
        }
        if (value instanceof org.hl7.fhir.dstu3.model.Enumeration<?> enumType) {
            return new org.hl7.fhir.dstu3.model.StringType(enumType.asStringValue());
        }
        if (value instanceof org.hl7.fhir.dstu3.model.IdType idType) {
            return new org.hl7.fhir.dstu3.model.StringType(idType.asStringValue());
        }
        if (value instanceof org.hl7.fhir.dstu3.model.Identifier identifier) {
            return new org.hl7.fhir.dstu3.model.StringType(identifier.getValue());
        }
        return value;
    }

    public static org.hl7.fhir.r4.model.Type transformValueToItem(org.hl7.fhir.r4.model.Type value) {
        if (value instanceof org.hl7.fhir.r4.model.CodeableConcept codeType) {
            return codeType.getCoding().get(0);
        }
        if (value instanceof org.hl7.fhir.r4.model.Enumeration<?> enumType) {
            return new org.hl7.fhir.r4.model.StringType(enumType.getCode());
        }
        if (value instanceof org.hl7.fhir.r4.model.IdType idType) {
            return new org.hl7.fhir.r4.model.StringType(idType.asStringValue());
        }
        if (value instanceof org.hl7.fhir.r4.model.Identifier identifier) {
            return new org.hl7.fhir.r4.model.StringType(identifier.getValue());
        }
        return value;
    }

    public static org.hl7.fhir.r5.model.DataType transformValueToItem(org.hl7.fhir.r5.model.DataType value) {
        if (value instanceof org.hl7.fhir.r5.model.CodeableConcept codeType) {
            return codeType.getCoding().get(0);
        }
        if (value instanceof org.hl7.fhir.r5.model.Enumeration<?> enumType) {
            return new org.hl7.fhir.r5.model.StringType(enumType.getCode());
        }
        if (value instanceof org.hl7.fhir.r5.model.IdType idType) {
            return new org.hl7.fhir.r5.model.StringType(idType.asStringValue());
        }
        if (value instanceof org.hl7.fhir.r5.model.Identifier identifier) {
            return new org.hl7.fhir.r5.model.StringType(identifier.getValue());
        }
        return value;
    }

    public static IBase transformValueToResource(FhirVersionEnum fhirVersion, IBase value) {
        return switch (fhirVersion) {
            case DSTU3 -> transformValueToResource((org.hl7.fhir.dstu3.model.Type) value);
            case R4 -> transformValueToResource((org.hl7.fhir.r4.model.Type) value);
            case R5 -> transformValueToResource((org.hl7.fhir.r5.model.DataType) value);
            default -> null;
        };
    }

    public static org.hl7.fhir.dstu3.model.Type transformValueToResource(org.hl7.fhir.dstu3.model.Type value) {
        if (value instanceof org.hl7.fhir.dstu3.model.Coding codingType) {
            return new org.hl7.fhir.dstu3.model.CodeableConcept().addCoding(codingType);
        }
        return value;
    }

    public static org.hl7.fhir.r4.model.Type transformValueToResource(org.hl7.fhir.r4.model.Type value) {
        if (value instanceof org.hl7.fhir.r4.model.Coding codingType) {
            return new org.hl7.fhir.r4.model.CodeableConcept().addCoding(codingType);
        }
        return value;
    }

    public static org.hl7.fhir.r5.model.DataType transformValueToResource(org.hl7.fhir.r5.model.DataType value) {
        if (value instanceof org.hl7.fhir.r5.model.Coding codingType) {
            return new org.hl7.fhir.r5.model.CodeableConcept().addCoding(codingType);
        }
        return value;
    }
}
