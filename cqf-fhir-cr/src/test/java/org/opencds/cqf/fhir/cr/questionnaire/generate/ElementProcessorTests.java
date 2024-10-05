package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newGenerateRequestForVersion;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createInitial;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createProcessor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.BooleanType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;

@ExtendWith(MockitoExtension.class)
class ElementProcessorTests {
    private final FhirContext fhirContextDstu2 = FhirContext.forDstu2Cached();
    private final FhirContext fhirContextR4B = FhirContext.forR4BCached();

    @Mock
    Repository repository;

    @Mock
    LibraryEngine libraryEngine;

    @Mock
    ExpressionProcessor expressionProcessor;

    @InjectMocks
    @Spy
    org.opencds.cqf.fhir.cr.questionnaire.generate.r4.ElementProcessor elementProcessorR4;

    @InjectMocks
    @Spy
    org.opencds.cqf.fhir.cr.questionnaire.generate.r5.ElementProcessor elementProcessorR5;

    @Test
    void nullElementTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> elementProcessorR4.parseItemType(null, false));
        assertThrows(IllegalArgumentException.class, () -> elementProcessorR5.parseItemType(null, false));
    }

    @Test
    void r4ItemTypes() {
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorR4.parseItemType("code", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorR4.parseItemType("coding", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorR4.parseItemType("CodeableConcept", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorR4.parseItemType("uri", true));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorR4.parseItemType("uri", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorR4.parseItemType("url", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorR4.parseItemType("canonical", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.QUANTITY,
                elementProcessorR4.parseItemType("Quantity", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.REFERENCE,
                elementProcessorR4.parseItemType("Reference", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorR4.parseItemType("oid", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorR4.parseItemType("uuid", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorR4.parseItemType("base64Binary", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.INTEGER,
                elementProcessorR4.parseItemType("positiveInt", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.INTEGER,
                elementProcessorR4.parseItemType("unsignedInt", false));
        assertEquals(
                org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DATETIME,
                elementProcessorR4.parseItemType("instant", false));
    }

    @Test
    void r5ItemTypes() {
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                elementProcessorR5.parseItemType("code", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                elementProcessorR5.parseItemType("coding", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                elementProcessorR5.parseItemType("CodeableConcept", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUESTION,
                elementProcessorR5.parseItemType("uri", true));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorR5.parseItemType("uri", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorR5.parseItemType("url", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorR5.parseItemType("canonical", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.QUANTITY,
                elementProcessorR5.parseItemType("Quantity", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.REFERENCE,
                elementProcessorR5.parseItemType("Reference", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorR5.parseItemType("oid", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorR5.parseItemType("uuid", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorR5.parseItemType("base64Binary", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.INTEGER,
                elementProcessorR5.parseItemType("positiveInt", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.INTEGER,
                elementProcessorR5.parseItemType("unsignedInt", false));
        assertEquals(
                org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DATETIME,
                elementProcessorR5.parseItemType("instant", false));
    }

    @Test
    void createProcessorShouldReturnNullForUnsupportedVersion() {
        doReturn(fhirContextR4B).when(repository).fhirContext();
        assertNull(createProcessor(repository));
    }

    @Test
    void createInitialShouldReturnNullForUnsupportedVersion() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(fhirContextDstu2).when(repository).fhirContext();
        var request = newGenerateRequestForVersion(FhirVersionEnum.DSTU2, libraryEngine);
        var initial = createInitial(request, new BooleanType(true));
        assertNull(initial);
    }
}
