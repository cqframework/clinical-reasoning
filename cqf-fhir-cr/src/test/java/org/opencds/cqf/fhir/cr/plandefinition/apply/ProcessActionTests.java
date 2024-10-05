package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;

@ExtendWith(MockitoExtension.class)
class ProcessActionTests {
    @Mock
    Repository repository;

    @Mock
    LibraryEngine libraryEngine;

    @Mock
    ModelResolver modelResolver;

    @Mock
    ApplyProcessor applyProcessor;

    @Mock
    GenerateProcessor generateProcessor;

    @InjectMocks
    @Spy
    ProcessAction fixture;

    @Test
    void unsupportedVersionShouldReturnNull() {
        var action = new org.hl7.fhir.r4b.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.R4B, action);
        assertNull(requestAction);
    }

    @Test
    void dstu3Request() {
        var action = new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.DSTU3, action);
        assertTrue(requestAction instanceof org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void r4Request() {
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.R4, action);
        assertTrue(requestAction instanceof org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void r5Request() {
        var action = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(FhirVersionEnum.R5, action);
        assertTrue(
                requestAction
                        instanceof org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent);
    }
}
