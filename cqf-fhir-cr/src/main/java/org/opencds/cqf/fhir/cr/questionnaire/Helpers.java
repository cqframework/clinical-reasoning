package org.opencds.cqf.fhir.cr.questionnaire;

import ca.uhn.fhir.context.FhirVersionEnum;

public class Helpers {
    private Helpers() {}

    public static Object parseItemTypeForVersion(
            FhirVersionEnum fhirVersion, String typeCode, Boolean hasBinding, Boolean isGroup) {
        switch (fhirVersion) {
            case R4:
                return parseR4ItemType(typeCode, hasBinding, isGroup);
            case R5:
                return parseR5ItemType(typeCode, hasBinding, isGroup);

            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s", fhirVersion));
        }
    }

    public static org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType parseR4ItemType(
            String elementType, Boolean hasBinding, Boolean isGroup) {
        if (Boolean.TRUE.equals(hasBinding)) {
            return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE;
        }
        if (Boolean.TRUE.equals(isGroup)) {
            return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.GROUP;
        }
        if (elementType == null) {
            return null;
        }
        switch (elementType) {
            case "code", "coding", "CodeableConcept":
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE;
            case "uri", "url", "canonical":
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL;
            case "Quantity":
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.QUANTITY;
            case "Reference":
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.REFERENCE;
            case "id", "oid", "uuid", "base64Binary", "Identifier", "Extension":
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING;
            case "positiveInt", "unsignedInt":
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.INTEGER;
            case "instant":
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DATETIME;
            default:
                return org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.fromCode(elementType);
        }
    }

    public static org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType parseR5ItemType(
            String elementType, Boolean hasBinding, Boolean isGroup) {
        if (Boolean.TRUE.equals(hasBinding)) {
            return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION;
        }
        if (Boolean.TRUE.equals(isGroup)) {
            return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.GROUP;
        }
        if (elementType == null) {
            return null;
        }
        switch (elementType) {
            case "code", "coding", "CodeableConcept":
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION;
            case "uri", "url", "canonical":
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL;
            case "Quantity":
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUANTITY;
            case "Reference":
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.REFERENCE;
            case "id", "oid", "uuid", "base64Binary", "Identifier", "Extension":
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING;
            case "positiveInt", "unsignedInt":
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.INTEGER;
            case "instant":
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DATETIME;
            default:
                return org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.fromCode(elementType);
        }
    }

    public static boolean isGroupItem(Object item) {
        if (item instanceof org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent r4Item) {
            return r4Item.getType() == org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.GROUP;
        }
        if (item instanceof org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent r5Item) {
            return r5Item.getType() == org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.GROUP;
        }
        return false;
    }

    public static boolean isGroupItemType(Object itemType) {
        if (itemType instanceof org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType r4ItemType) {
            return r4ItemType == org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.GROUP;
        }
        if (itemType instanceof org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType r5ItemType) {
            return r5ItemType == org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.GROUP;
        }
        return false;
    }

    public static boolean isChoiceItemType(Object itemType) {
        if (itemType instanceof org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType r4ItemType) {
            return r4ItemType == org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE;
        }
        if (itemType instanceof org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType r5ItemType) {
            return r5ItemType == org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION;
        }
        return false;
    }

    public static String getSliceName(String elementId) {
        if (!elementId.contains(":")) {
            return null;
        }
        var sliceName = elementId.substring(elementId.indexOf(":") + 1);
        if (sliceName.contains(".")) {
            sliceName = sliceName.substring(0, sliceName.indexOf("."));
        }
        return sliceName;
    }
}
