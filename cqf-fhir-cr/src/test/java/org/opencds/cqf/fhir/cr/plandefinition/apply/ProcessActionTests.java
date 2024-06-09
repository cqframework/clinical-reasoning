package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @Test
    void unsupportedVersionShouldReturnNull() {
        doReturn(FhirContext.forR4BCached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4B, libraryEngine, modelResolver);
        var action = new org.hl7.fhir.r4b.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertNull(requestAction);
    }

    @Test
    void dstu3Request() {
        doReturn(FhirContext.forDstu3Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine);
        var action = new org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertTrue(requestAction instanceof org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void r4Request() {
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var action = new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertTrue(requestAction instanceof org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent);
    }

    @Test
    void r5Request() {
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5, libraryEngine);
        var action = new org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent();
        var requestAction = fixture.generateRequestAction(request, action);
        assertTrue(
                requestAction
                        instanceof org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent);
    }
}
