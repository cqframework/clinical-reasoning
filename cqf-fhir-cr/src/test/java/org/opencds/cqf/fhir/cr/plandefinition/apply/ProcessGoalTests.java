package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Coding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;

@ExtendWith(MockitoExtension.class)
class ProcessGoalTests {
    @Mock
    Repository repository;

    @Mock
    LibraryEngine libraryEngine;

    @Mock
    ModelResolver modelResolver;

    ProcessGoal fixture = new ProcessGoal();

    @Test
    void unsupportedVersionShouldReturnNull() {
        var request = mock(ApplyRequest.class);
        doReturn(FhirVersionEnum.R4B).when(request).getFhirVersion();
        var goalElement = new org.hl7.fhir.r4b.model.PlanDefinition.PlanDefinitionGoalComponent();
        var result = fixture.convertGoal(request, goalElement);
        assertNull(result);
    }

    @Test
    void convertDstu3Goal() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forDstu3Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine, null);
        var goalElement = new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionGoalTargetComponent()
                        .setDetail(new org.hl7.fhir.dstu3.model.CodeableConcept(new Coding("test", "test", "test"))));
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.dstu3.model.Goal);
        var goal = (org.hl7.fhir.dstu3.model.Goal) result;
        assertNotNull(goal);
    }

    @Test
    void convertR4Goal() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var goalElement = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalTargetComponent());
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.r4.model.Goal);
        var goal = (org.hl7.fhir.r4.model.Goal) result;
        assertNotNull(goal);
    }

    @Test
    void convertR5Goal() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5, libraryEngine, null);
        var goalElement = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionGoalTargetComponent());
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.r5.model.Goal);
        var goal = (org.hl7.fhir.r5.model.Goal) result;
        assertNotNull(goal);
    }
}
