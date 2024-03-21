package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newGenerateRequestForVersion;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createInitial;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createProcessor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
public class ElementProcessorTests {
    private final FhirContext fhirContextDstu2 = FhirContext.forDstu2Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR4B = FhirContext.forR4BCached();

    @Mock
    Repository repository;

    @Mock
    LibraryEngine libraryEngine;

    @Mock
    ExpressionProcessor expressionProcessor;

    @InjectMocks
    @Spy
    org.opencds.cqf.fhir.cr.questionnaire.generate.dstu3.ElementProcessor elementProcessorDstu3;

    @InjectMocks
    @Spy
    org.opencds.cqf.fhir.cr.questionnaire.generate.r4.ElementProcessor elementProcessorR4;

    @InjectMocks
    @Spy
    org.opencds.cqf.fhir.cr.questionnaire.generate.r5.ElementProcessor elementProcessorR5;

    @Test
    void testNullElementTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> elementProcessorDstu3.parseItemType(null, false));
        assertThrows(IllegalArgumentException.class, () -> elementProcessorR4.parseItemType(null, false));
        assertThrows(IllegalArgumentException.class, () -> elementProcessorR5.parseItemType(null, false));
    }

    @Test
    void testDstu3ItemTypes() {
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorDstu3.parseItemType("code", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorDstu3.parseItemType("coding", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorDstu3.parseItemType("CodeableConcept", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.CHOICE,
                elementProcessorDstu3.parseItemType("uri", true));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorDstu3.parseItemType("uri", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.URL,
                elementProcessorDstu3.parseItemType("url", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.QUANTITY,
                elementProcessorDstu3.parseItemType("Quantity", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.REFERENCE,
                elementProcessorDstu3.parseItemType("Reference", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorDstu3.parseItemType("oid", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorDstu3.parseItemType("uuid", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.STRING,
                elementProcessorDstu3.parseItemType("base64Binary", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.INTEGER,
                elementProcessorDstu3.parseItemType("positiveInt", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.INTEGER,
                elementProcessorDstu3.parseItemType("unsignedInt", false));
        assertEquals(
                org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.DATETIME,
                elementProcessorDstu3.parseItemType("instant", false));
    }

    @Test
    void testR4ItemTypes() {
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
    void testR5ItemTypes() {
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

    @Test
    void testElementWithCqfExpressionWithResourceResult() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(fhirContextR4).when(repository).fhirContext();
        var request = newGenerateRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var cqfExpression = new CqfExpression();
        var expectedResource = new Patient().setId("test");
        var item = new QuestionnaireItemComponent()
                .setLinkId("test")
                .setType(org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.REFERENCE);
        doReturn(cqfExpression)
                .when(expressionProcessor)
                .getCqfExpression(request, Collections.emptyList(), Constants.CQF_EXPRESSION);
        doReturn(Collections.singletonList(expectedResource))
                .when(expressionProcessor)
                .getExpressionResult(request, cqfExpression);
        var actual = (QuestionnaireItemComponent)
                new ElementHasCqfExpression(expressionProcessor).addProperties(request, Collections.emptyList(), item);
        assertNotNull(actual);
        assertTrue(actual.hasInitial());
        assertEquals("test", actual.getInitial().get(0).getValueReference().getReference());
    }
}
