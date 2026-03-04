package org.opencds.cqf.fhir.cr.questionnaire;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter;

public class Helpers {
    public static final String QUESTIONNAIRE = "Questionnaire";
    private static final String CHOICE = "choice";
    private static final String QUESTION = "question";
    private static final String GROUP = "group";
    private static final String URL = "url";
    private static final String QUANTITY = "quantity";
    private static final String REFERENCE = "reference";
    private static final String STRING = "string";
    private static final String INTEGER = "integer";
    private static final String DATETIME = "dateTime";

    private Helpers() {}

    public static String parseItemTypeForVersion(
            FhirVersionEnum fhirVersion, String typeCode, Boolean hasBinding, Boolean isGroup) {
        if (Boolean.TRUE.equals(hasBinding)) {
            return fhirVersion.isEqualOrNewerThan(FhirVersionEnum.R5) ? QUESTION : CHOICE;
        }
        if (Boolean.TRUE.equals(isGroup)) {
            return GROUP;
        }
        if (StringUtils.isBlank(typeCode)) {
            return null;
        }
        return switch (typeCode) {
            case "code", "coding", "CodeableConcept" ->
                fhirVersion.isEqualOrNewerThan(FhirVersionEnum.R5) ? QUESTION : CHOICE;
            case "uri", "url", "canonical" -> URL;
            case "Quantity" -> QUANTITY;
            case "Reference" -> REFERENCE;
            case "id", "oid", "uuid", "base64Binary", "Identifier", "Extension" -> STRING;
            case "positiveInt", "unsignedInt" -> INTEGER;
            case "instant" -> DATETIME;
            default -> typeCode;
        };
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

    public static IBaseResource getQuestionnaireFromContained(IQuestionnaireResponseAdapter questionnaireResponse) {
        return questionnaireResponse == null
                ? null
                : questionnaireResponse.getContained().stream()
                        .filter(r -> r.fhirType().equals(QUESTIONNAIRE)
                                && questionnaireResponse
                                        .getQuestionnaire()
                                        .equals(getContainedId(r.getIdElement().getIdPart())))
                        .findFirst()
                        .orElse(null);
    }

    private static String getContainedId(String id) {
        return id.startsWith("#") ? id : "#" + id;
    }
}
