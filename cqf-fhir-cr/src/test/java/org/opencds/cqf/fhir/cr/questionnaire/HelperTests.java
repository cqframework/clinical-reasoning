package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class HelperTests {
    @Test
    void testNullElementType() {
        assertNull(Helpers.parseR4ItemType(null, false, false));
        assertNotNull(Helpers.parseR4ItemType(null, true, false));
        assertNotNull(Helpers.parseR4ItemType(null, false, true));
        assertNull(Helpers.parseR5ItemType(null, false, false));
        assertNotNull(Helpers.parseR5ItemType(null, true, false));
        assertNotNull(Helpers.parseR5ItemType(null, false, true));
        // assertThrows(IllegalArgumentException.class, () -> Helpers.parseR4ItemType(null, false, false));
        // assertThrows(IllegalArgumentException.class, () -> Helpers.parseR5ItemType(null, false, false));
    }

    @Test
    void r4ItemTypes() {
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                Helpers.parseR4ItemType("code", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                Helpers.parseR4ItemType("coding", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                Helpers.parseR4ItemType("CodeableConcept", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                Helpers.parseR4ItemType("uri", true, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL,
                Helpers.parseR4ItemType("uri", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL,
                Helpers.parseR4ItemType("url", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL,
                Helpers.parseR4ItemType("canonical", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.QUANTITY,
                Helpers.parseR4ItemType("Quantity", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.REFERENCE,
                Helpers.parseR4ItemType("Reference", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING,
                Helpers.parseR4ItemType("oid", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING,
                Helpers.parseR4ItemType("uuid", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING,
                Helpers.parseR4ItemType("base64Binary", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.INTEGER,
                Helpers.parseR4ItemType("positiveInt", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.INTEGER,
                Helpers.parseR4ItemType("unsignedInt", false, false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DATETIME,
                Helpers.parseR4ItemType("instant", false, false));
    }

    @Test
    void r5ItemTypes() {
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                Helpers.parseR5ItemType("code", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                Helpers.parseR5ItemType("coding", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                Helpers.parseR5ItemType("CodeableConcept", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                Helpers.parseR5ItemType("uri", true, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL,
                Helpers.parseR5ItemType("uri", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL,
                Helpers.parseR5ItemType("url", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL,
                Helpers.parseR5ItemType("canonical", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUANTITY,
                Helpers.parseR5ItemType("Quantity", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.REFERENCE,
                Helpers.parseR5ItemType("Reference", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING,
                Helpers.parseR5ItemType("oid", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING,
                Helpers.parseR5ItemType("uuid", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING,
                Helpers.parseR5ItemType("base64Binary", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.INTEGER,
                Helpers.parseR5ItemType("positiveInt", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.INTEGER,
                Helpers.parseR5ItemType("unsignedInt", false, false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DATETIME,
                Helpers.parseR5ItemType("instant", false, false));
    }
}
