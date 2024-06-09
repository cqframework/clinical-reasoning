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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;

@ExtendWith(MockitoExtension.class)
class ProcessRequestTests {
    @Mock
    Repository repository;

    @Mock
    LibraryEngine libraryEngine;

    @Mock
    ModelResolver modelResolver;

    @Mock
    IPopulateProcessor populateProcessor;

    ProcessRequest fixture = new ProcessRequest(populateProcessor);

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @Test
    void unsupportedVersionShouldReturnNull() {
        doReturn(FhirContext.forR4BCached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4B, libraryEngine, modelResolver);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertNull(requestOrchestration);
        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertNull(carePlan);
    }

    @Test
    void dstu3Request() {
        doReturn(FhirContext.forDstu3Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertTrue(requestOrchestration instanceof org.hl7.fhir.dstu3.model.RequestGroup);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertTrue(carePlan instanceof org.hl7.fhir.dstu3.model.CarePlan);
    }

    @Test
    void r4Request() {
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertTrue(requestOrchestration instanceof org.hl7.fhir.r4.model.RequestGroup);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertTrue(carePlan instanceof org.hl7.fhir.r4.model.CarePlan);
    }

    @Test
    void r5Request() {
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5, libraryEngine);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertTrue(requestOrchestration instanceof org.hl7.fhir.r5.model.RequestOrchestration);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertTrue(carePlan instanceof org.hl7.fhir.r5.model.CarePlan);
    }
}
