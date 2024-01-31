package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessGoalTests {
    @Spy
    ProcessGoal fixture;

    @Test
    void unsupportedVersionShouldReturnNull() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4B);
        var goalElement = new org.hl7.fhir.r4b.model.PlanDefinition.PlanDefinitionGoalComponent();
        var result = fixture.convertGoal(request, goalElement);
        assertNull(result);
    }

    @Test
    void testConvertDstu3Goal() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3);
        var goalElement = new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionGoalTargetComponent());
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.dstu3.model.Goal);
        var goal = (org.hl7.fhir.dstu3.model.Goal) result;
        assertNotNull(goal);
    }

    @Test
    void testConvertR4Goal() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4);
        var goalElement = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalTargetComponent());
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.r4.model.Goal);
        var goal = (org.hl7.fhir.r4.model.Goal) result;
        assertNotNull(goal);
    }

    @Test
    void testConvertR5Goal() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5);
        var goalElement = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionGoalComponent()
                .addTarget(new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionGoalTargetComponent());
        var result = fixture.convertGoal(request, goalElement);
        assertTrue(result instanceof org.hl7.fhir.r5.model.Goal);
        var goal = (org.hl7.fhir.r5.model.Goal) result;
        assertNotNull(goal);
    }
}
