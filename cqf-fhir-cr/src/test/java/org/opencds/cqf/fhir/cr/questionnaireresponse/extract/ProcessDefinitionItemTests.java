package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.Ids;

@ExtendWith(MockitoExtension.class)
class ProcessDefinitionItemTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Mock
    private Repository repository;

    @Mock
    ExpressionProcessor expressionProcessor;

    @Mock
    private LibraryEngine libraryEngine;

    private ProcessDefinitionItem fixture;

    @BeforeEach
    void setup() {
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(repository).when(libraryEngine).getRepository();
        fixture = new ProcessDefinitionItem(expressionProcessor);
    }

    @Test
    void testItemWithNoDefinitionThrows() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent();
        var responseItem = new QuestionnaireResponseItemComponent();
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var resources = new ArrayList<IBaseResource>();
        var subjectId = Ids.newId(fhirVersion, "Patient", "patient1");
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.processDefinitionItem(request, responseItem, item, resources, new Reference(subjectId)));
    }

    @Test
    void testItemWithContextExtensionWithType() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent().setLinkId("1");
        item.addExtension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT, new CodeType("Condition"));
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var resources = new ArrayList<IBaseResource>();
        var subjectId = Ids.newId(fhirVersion, "Patient", "patient1");
        fixture.processDefinitionItem(request, responseItem, item, resources, new Reference(subjectId));
        assertTrue(resources.size() == 1);
        assertTrue(resources.get(0).fhirType().equals("Condition"));
    }

    @Test
    void testItemWithContextExtensionWithExpressionFailureThrows() throws ResolveExpressionException {
        var fhirVersion = FhirVersionEnum.R4;
        var patientId = "patient1";
        var expression = new CqfExpression().setLanguage("text/fhirpath").setExpression("resource");
        var item = new QuestionnaireItemComponent().setLinkId("1");
        var extension = new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                .setValue(new Expression()
                        .setLanguage(expression.getLanguage())
                        .setExpression(expression.getExpression()));
        item.addExtension(extension);
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var resources = new ArrayList<IBaseResource>();
        var subjectId = Ids.newId(fhirVersion, "Patient", patientId);
        doThrow(ResolveExpressionException.class)
                .when(expressionProcessor)
                .getExpressionResultForItem(eq(request), any(), eq("1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.processDefinitionItem(request, responseItem, item, resources, new Reference(subjectId)));
    }

    @Test
    void testItemWithContextExtensionWithResource() throws ResolveExpressionException {
        var fhirVersion = FhirVersionEnum.R4;
        var patientId = "patient1";
        var expression = new CqfExpression().setLanguage("text/fhirpath").setExpression("resource");
        var item = new QuestionnaireItemComponent().setLinkId("1");
        var extension = new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                .setValue(new Expression()
                        .setLanguage(expression.getLanguage())
                        .setExpression(expression.getExpression()));
        item.addExtension(extension);
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var resources = new ArrayList<IBaseResource>();
        var subjectId = Ids.newId(fhirVersion, "Patient", patientId);
        var expectedCondition = (IBase) new Condition();
        doReturn(Collections.singletonList(expectedCondition))
                .when(expressionProcessor)
                .getExpressionResultForItem(eq(request), any(), eq("1"));
        fixture.processDefinitionItem(request, responseItem, item, resources, new Reference(subjectId));
        assertTrue(resources.size() == 1);
        assertEquals(expectedCondition, resources.get(0));
    }

    @Test
    void testItemWithContextExtensionWithMultipleResources() throws ResolveExpressionException {
        var fhirVersion = FhirVersionEnum.R4;
        var patientId = "patient1";
        var expression = new CqfExpression().setLanguage("text/fhirpath").setExpression("resource");
        var item = new QuestionnaireItemComponent().setLinkId("1");
        var extension = new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                .setValue(new Expression()
                        .setLanguage(expression.getLanguage())
                        .setExpression(expression.getExpression()));
        item.addExtension(extension);
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var resources = new ArrayList<IBaseResource>();
        var subjectId = Ids.newId(fhirVersion, "Patient", patientId);
        var expectedCondition1 = (IBase) new Condition();
        var expectedCondition2 = (IBase) new Condition();
        List<IBase> expectedConditions = Arrays.asList(expectedCondition1, expectedCondition2);
        doReturn(expectedConditions).when(expressionProcessor).getExpressionResultForItem(eq(request), any(), eq("1"));
        fixture.processDefinitionItem(request, responseItem, item, resources, new Reference(subjectId));
        assertTrue(resources.size() == 2);
        assertEquals(expectedCondition1, resources.get(0));
        assertEquals(expectedCondition2, resources.get(1));
    }
}
