package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ProcessDefinitionTests {
    private static final String ACTIVITYDEFINITION = "http://test.fhir.org/fhir/ActivityDefinition/test";
    private static final String PLANDEFINITION = "http://test.fhir.org/fhir/PlanDefinition/test";
    private static final String QUESTIONNAIRE = "http://test.fhir.org/fhir/Questionnaire/test";
    private static final String TASK = "http://test.fhir.org/fhir/Task/test";

    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Mock
    IRepository repository;

    @Mock
    LibraryEngine libraryEngine;

    @Mock
    ApplyProcessor applyProcessor;

    @Spy
    @InjectMocks
    ProcessDefinition fixture;

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
    }

    @Test
    void applyActivityDefinitionShouldReturnNullOnException() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var definition = new CanonicalType(ACTIVITYDEFINITION);
        doReturn(fhirContextR4).when(repository).fhirContext();
        var result = fixture.applyActivityDefinition(request, definition);
        assertNull(result);
        var oc = (OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
    }

    @Test
    void applyNestedPlanDefinitionShouldReturnNullOnException() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var definition = new CanonicalType(PLANDEFINITION);
        doReturn(fhirContextR4).when(repository).fhirContext();
        var result = fixture.applyNestedPlanDefinition(request, definition);
        assertNull(result);
        var oc = (OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
    }

    @Test
    void resolveDefinitionShouldReturnQuestionnaire() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var definition = new CanonicalType(QUESTIONNAIRE);
        var expectedQuestionnaire = new Questionnaire().setUrl(QUESTIONNAIRE);
        doReturn(expectedQuestionnaire).when(fixture).resolveRepository(definition);
        var result = fixture.resolveDefinition(request, definition);
        assertEquals(expectedQuestionnaire, result);
        var oc = request.getOperationOutcome();
        assertNull(oc);
    }

    @Test
    void applyQuestionnaireDefinitionShouldReturnContainedQuestionnaire() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var definition = new CanonicalType("#Questionnaire/test");
        var expectedQuestionnaire = new Questionnaire().setUrl(QUESTIONNAIRE);
        doReturn(expectedQuestionnaire).when(fixture).resolveContained(request, definition.getValue());
        var result = fixture.applyQuestionnaireDefinition(request, definition);
        assertEquals(expectedQuestionnaire, result);
        var oc = request.getOperationOutcome();
        assertNull(oc);
    }

    @Test
    void applyQuestionnaireDefinitionShouldReturnNullOnException() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var definition = new CanonicalType(QUESTIONNAIRE);
        doReturn(fhirContextR4).when(repository).fhirContext();
        var result = fixture.applyQuestionnaireDefinition(request, definition);
        assertNull(result);
        var oc = (OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
    }

    @Test
    void resolveDefinitionShouldFailOnInvalidAction() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var definition = new CanonicalType(TASK);
        doReturn("Task").when(fixture).resolveResourceName(request, definition);
        assertThrows(FHIRException.class, () -> {
            fixture.resolveDefinition(request, definition);
        });
    }

    @Test
    void resolveResourceNameShouldFailIfCanonicalHasNoValue() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        final CanonicalType canonical = new CanonicalType();
        assertThrows(FHIRException.class, () -> fixture.resolveResourceName(request, canonical));
    }

    @Test
    void applyActivityDefinitionHandlesMultipleRequestResources() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null, null, null);
        var definition = new CanonicalType(ACTIVITYDEFINITION);
        var activityDef = new ActivityDefinition().setUrl(ACTIVITYDEFINITION);
        activityDef.setId("test");
        var task = new Task();
        task.setId("test");
        request.getRequestResources().add(task);
        var newTask = new Task();
        newTask.setId("test");
        doReturn(activityDef).when(fixture).resolveRepository(any());
        doReturn(newTask).when(applyProcessor).applyActivityDefinition(any());
        var result = fixture.applyActivityDefinition(request, definition);
        assertInstanceOf(Task.class, result);
        assertEquals("test2", result.getIdElement().getIdPart());
    }
}
