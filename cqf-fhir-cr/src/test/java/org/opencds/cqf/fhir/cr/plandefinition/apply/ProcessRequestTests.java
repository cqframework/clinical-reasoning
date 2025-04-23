package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPDApplyRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.RequestGroup;
import org.hl7.fhir.r5.model.RequestOrchestration;
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

    ResponseBuilder fixture = new ResponseBuilder(populateProcessor);

    @Test
    void unsupportedVersionShouldReturnNull() {
        var request = mock(ApplyRequest.class);
        doReturn(FhirVersionEnum.R4B).when(request).getFhirVersion();
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertNull(requestOrchestration);
        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertNull(carePlan);
    }

    @Test
    void dstu3Request() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forDstu3Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine, null);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertInstanceOf(RequestGroup.class, requestOrchestration);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertInstanceOf(CarePlan.class, carePlan);
    }

    @Test
    void r4Request() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, null);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertInstanceOf(org.hl7.fhir.r4.model.RequestGroup.class, requestOrchestration);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertInstanceOf(org.hl7.fhir.r4.model.CarePlan.class, carePlan);
    }

    @Test
    void r5Request() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        var request = newPDApplyRequestForVersion(FhirVersionEnum.R5, libraryEngine, null);
        var requestOrchestration = fixture.generateRequestOrchestration(request);
        assertInstanceOf(RequestOrchestration.class, requestOrchestration);

        var carePlan = fixture.generateCarePlan(request, requestOrchestration);
        assertInstanceOf(org.hl7.fhir.r5.model.CarePlan.class, carePlan);
    }
}
