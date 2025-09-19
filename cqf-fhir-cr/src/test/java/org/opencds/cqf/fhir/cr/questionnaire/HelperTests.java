package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;

class HelperTests {
    FhirVersionEnum fhirVersionR4 = FhirVersionEnum.R4;
    FhirVersionEnum fhirVersionR5 = FhirVersionEnum.R5;

    @Test
    void testNullElementType() {
        assertNull(Helpers.parseItemTypeForVersion(fhirVersionR4, null, false, false));
        assertNotNull(Helpers.parseItemTypeForVersion(fhirVersionR4, null, true, false));
        assertNotNull(Helpers.parseItemTypeForVersion(fhirVersionR4, null, false, true));
        assertNull(Helpers.parseItemTypeForVersion(fhirVersionR5, null, false, false));
        assertNotNull(Helpers.parseItemTypeForVersion(fhirVersionR5, null, true, false));
        assertNotNull(Helpers.parseItemTypeForVersion(fhirVersionR5, null, false, true));
    }

    @Test
    void r4ItemTypes() {
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "code", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "coding", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "CodeableConcept", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "uri", true, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "uri", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "url", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "canonical", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.QUANTITY.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "Quantity", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.REFERENCE.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "Reference", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "oid", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "uuid", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "base64Binary", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.INTEGER.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "positiveInt", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.INTEGER.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "unsignedInt", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DATETIME.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR4, "instant", false, false));
    }

    @Test
    void r5ItemTypes() {
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "code", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "coding", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "CodeableConcept", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "uri", true, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "uri", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "url", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "canonical", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUANTITY.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "Quantity", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.REFERENCE.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "Reference", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "oid", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "uuid", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "base64Binary", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.INTEGER.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "positiveInt", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.INTEGER.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "unsignedInt", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DATETIME.toCode(),
                Helpers.parseItemTypeForVersion(fhirVersionR5, "instant", false, false));
    }
}
