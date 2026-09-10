package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var activityDef = new ActivityDefinition().setUrl(ACTIVITYDEFINITION);
        activityDef.setId("test");
        var result = fixture.applyActivityDefinition(request, activityDef);
        assertNull(result);
        var oc = (OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
    }

    @Test
    void applyNestedPlanDefinitionShouldReturnNullOnException() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var nestedPlanDef = new PlanDefinition().setUrl(PLANDEFINITION);
        nestedPlanDef.setId("nested");
        doThrow(new RuntimeException("boom")).when(applyProcessor).applyPlanDefinition(any());
        var result = fixture.applyNestedPlanDefinition(request, nestedPlanDef);
        assertNull(result);
        var oc = (OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
    }

    @Test
    void resolveDefinitionShouldReturnQuestionnaire() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType(QUESTIONNAIRE);
        var expectedQuestionnaire = new Questionnaire().setUrl(QUESTIONNAIRE);
        expectedQuestionnaire.setId("q1");
        doReturn(toSearchBundle(List.of(expectedQuestionnaire)))
                .when(fixture)
                .searchByCanonicalType(eq("Questionnaire"), eq(QUESTIONNAIRE), isNull());
        var result = fixture.resolveDefinition(request, definition);
        assertEquals(expectedQuestionnaire, result);
        var oc = request.getOperationOutcome();
        assertNull(oc);
    }

    @Test
    void applyQuestionnaireDefinitionShouldReturnContainedQuestionnaire() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("#Questionnaire/test");
        var expectedQuestionnaire = new Questionnaire().setUrl(QUESTIONNAIRE);
        doReturn(expectedQuestionnaire).when(fixture).resolveContained(request, definition.getValue());
        var result = fixture.resolveDefinition(request, definition);
        assertEquals(expectedQuestionnaire, result);
        var oc = request.getOperationOutcome();
        assertNull(oc);
    }

    @Test
    void resolveDefinitionReturnsNullWhenCanonicalNotFound() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Foo/test");
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(any(Bundle.class));
        var result = fixture.resolveDefinition(request, definition);
        assertNull(result);
        var oc = (OperationOutcome) request.getOperationOutcome();
        assertTrue(oc.hasIssue());
    }

    @Test
    void resolveDefinitionThrowsWhenMultipleCanonicalMatches() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Foo/test");
        var a = new PlanDefinition().setUrl("http://test.fhir.org/fhir/Foo/test");
        var b = new PlanDefinition().setUrl("http://test.fhir.org/fhir/Foo/test");
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(a, b), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(any(Bundle.class));

        var ex = assertThrows(IllegalStateException.class, () -> fixture.resolveDefinition(request, definition));
        assertTrue(ex.getMessage().contains("Multiple resources (2)"));
        assertTrue(ex.getMessage().contains("Specify a version"));
    }

    @Test
    void resolveDefinitionAmbiguityErrorMentionsVersionWhenVersionSpecified() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Foo/test|1.0.0");
        var a = new PlanDefinition()
                .setUrl("http://test.fhir.org/fhir/Foo/test")
                .setVersion("1.0.0");
        var b = new PlanDefinition()
                .setUrl("http://test.fhir.org/fhir/Foo/test")
                .setVersion("1.0.0");
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(a, b), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(any(Bundle.class));

        var ex = assertThrows(IllegalStateException.class, () -> fixture.resolveDefinition(request, definition));
        assertTrue(ex.getMessage().contains("Even with the specified version"));
    }

    @Test
    void resolveDefinitionSendsTransactionWithSearchEntriesForAllSupportedTypes() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Foo/test");
        var captor = ArgumentCaptor.forClass(Bundle.class);
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(captor.capture());

        fixture.resolveDefinition(request, definition);

        var sent = captor.getValue();
        assertEquals(Bundle.BundleType.TRANSACTION, sent.getType());
        assertEquals(6, sent.getEntry().size());
        var urls = sent.getEntry().stream().map(e -> e.getRequest().getUrl()).toList();
        assertTrue(urls.contains("Questionnaire?url=http://test.fhir.org/fhir/Foo/test"));
        assertTrue(urls.contains("ActivityDefinition?url=http://test.fhir.org/fhir/Foo/test"));
        assertTrue(urls.contains("PlanDefinition?url=http://test.fhir.org/fhir/Foo/test"));
        assertTrue(urls.contains("MessageDefinition?url=http://test.fhir.org/fhir/Foo/test"));
        assertTrue(urls.contains("ObservationDefinition?url=http://test.fhir.org/fhir/Foo/test"));
        assertTrue(urls.contains("SpecimenDefinition?url=http://test.fhir.org/fhir/Foo/test"));
        sent.getEntry()
                .forEach(e -> assertEquals(Bundle.HTTPVerb.GET, e.getRequest().getMethod()));
    }

    @Test
    void resolveDefinitionIncludesVersionInSearchUrlsWhenProvided() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Foo/test|1.2.3");
        var captor = ArgumentCaptor.forClass(Bundle.class);
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(captor.capture());

        fixture.resolveDefinition(request, definition);

        var urls = captor.getValue().getEntry().stream()
                .map(e -> e.getRequest().getUrl())
                .toList();
        urls.forEach(u -> assertTrue(u.contains("&version=1.2.3"), "Expected version param in: " + u));
        assertTrue(urls.contains("PlanDefinition?url=http://test.fhir.org/fhir/Foo/test&version=1.2.3"));
    }

    @Test
    void resolveDefinitionOmitsVersionInSearchUrlsWhenAbsent() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Foo/test");
        var captor = ArgumentCaptor.forClass(Bundle.class);
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(captor.capture());

        fixture.resolveDefinition(request, definition);

        captor.getValue().getEntry().stream()
                .map(e -> e.getRequest().getUrl())
                .forEach(u -> assertFalse(u.contains("version="), "Did not expect version param in: " + u));
    }

    @Test
    void resolveDefinitionReturnsResourceAsIsForUnsupportedFhirType() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Library/test");
        var library = new Library().setUrl("http://test.fhir.org/fhir/Library/test");
        library.setId("lib-1");
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(library), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(any(Bundle.class));

        var result = fixture.resolveDefinition(request, definition);

        assertNotNull(result);
        assertEquals("Library", result.fhirType());
        assertEquals(library, result);
    }

    @Test
    void resolveDefinitionRoutesByFhirTypeNotByUrl() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var definition = new CanonicalType("http://test.fhir.org/fhir/Foo/test");
        var planDef = new PlanDefinition().setUrl("http://test.fhir.org/fhir/Foo/test");
        planDef.setId("nested-1");
        doReturn(buildTransactionResponse(List.of(), List.of(), List.of(planDef), List.of(), List.of(), List.of()))
                .when(repository)
                .transaction(any(Bundle.class));
        var carePlan = new CarePlan();
        carePlan.setId("cp-1");
        doReturn(carePlan).when(applyProcessor).applyPlanDefinition(any());

        var result = fixture.resolveDefinition(request, definition);

        assertEquals(carePlan, result);
    }

    private static Bundle buildTransactionResponse(
            List<? extends Resource> questionnaireMatches,
            List<? extends Resource> activityDefinitionMatches,
            List<? extends Resource> planDefinitionMatches,
            List<? extends Resource> messageDefinitionMatches,
            List<? extends Resource> observationDefinitionMatches,
            List<? extends Resource> specimenDefinitionMatches) {
        var response = new Bundle();
        response.addEntry().setResource(toSearchBundle(questionnaireMatches));
        response.addEntry().setResource(toSearchBundle(activityDefinitionMatches));
        response.addEntry().setResource(toSearchBundle(planDefinitionMatches));
        response.addEntry().setResource(toSearchBundle(messageDefinitionMatches));
        response.addEntry().setResource(toSearchBundle(observationDefinitionMatches));
        response.addEntry().setResource(toSearchBundle(specimenDefinitionMatches));
        return response;
    }

    private static Bundle toSearchBundle(List<? extends Resource> resources) {
        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        for (var r : resources) {
            bundle.addEntry().setResource(r);
        }
        return bundle;
    }

    @Test
    void applyActivityDefinitionHandlesMultipleRequestResources() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null, null);
        var definition = new CanonicalType(ACTIVITYDEFINITION);
        var activityDef = new ActivityDefinition().setUrl(ACTIVITYDEFINITION);
        activityDef.setId("test");
        var task = new Task();
        task.setId("test");
        request.getRequestResources().add(task);
        var newTask = new Task();
        newTask.setId("test");
        doReturn(newTask).when(applyProcessor).applyActivityDefinition(any());
        var result = fixture.applyActivityDefinition(request, activityDef);
        assertInstanceOf(Task.class, result);
        assertEquals("test2", result.getIdElement().getIdPart());
    }
}
