package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Coding;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @Test
    void unsupportedVersionShouldReturnNull() {
        doReturn(FhirContext.forR4BCached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4B, libraryEngine, modelResolver);
        var goalElement = new org.hl7.fhir.r4b.model.PlanDefinition.PlanDefinitionGoalComponent();
        var result = fixture.convertGoal(request, goalElement);
        assertNull(result);
    }

    @Test
    void convertDstu3Goal() {
        doReturn(FhirContext.forDstu3Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine);
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
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var goalElement = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalTargetComponent());
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.r4.model.Goal);
        var goal = (org.hl7.fhir.r4.model.Goal) result;
        assertNotNull(goal);
    }

    @Test
    void convertR5Goal() {
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5, libraryEngine);
        var goalElement = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionGoalTargetComponent());
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.r5.model.Goal);
        var goal = (org.hl7.fhir.r5.model.Goal) result;
        assertNotNull(goal);
    }
}
