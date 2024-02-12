package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;

@ExtendWith(MockitoExtension.class)
public class ProcessActionTests {
    @Mock
    Repository repository;

    @Mock
    ApplyProcessor applyProcessor;

    @Mock
    GenerateProcessor generateProcessor;

    @InjectMocks
    @Spy
    ProcessAction fixture;

    @Test
    void unsupportedVersionShouldReturnNull() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4B);
        var action = new org.hl7.fhir.r4b.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertNull(requestAction);
    }

    @Test
    void testDstu3Request() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3);
        var action = new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertTrue(requestAction instanceof org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void testR4Request() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4);
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertTrue(requestAction instanceof org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void testR5Request() {
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5);
        var action = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertTrue(
                requestAction
                        instanceof org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent);
    }
}
