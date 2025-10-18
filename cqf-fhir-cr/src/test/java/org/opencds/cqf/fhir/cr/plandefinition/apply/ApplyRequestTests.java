package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Constants;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ApplyRequestTests {

    @Mock
    private IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Mock
    private ModelResolver modelResolver;

    @Mock
    private IInputParameterResolver inputParameterResolver;

    @Test
    void invalidVersionReturnsNull() {
        var request = mock(ApplyRequest.class, CALLS_REAL_METHODS);
        doReturn(FhirVersionEnum.R4B).when(request).getFhirVersion();
        var activityDef = new org.hl7.fhir.r4b.model.ActivityDefinition();
        assertNull(request.transformRequestParameters(activityDef));
    }

    @Test
    void transformRequestParametersDstu3() {
        var params = org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part("param", "true"));
        var bundle = new org.hl7.fhir.dstu3.model.Bundle();
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.DSTU3, libraryEngine, null, null, inputParameterResolver)
                .setData(bundle);
        var activityDef = new org.hl7.fhir.dstu3.model.ActivityDefinition();
        doReturn(params).when(inputParameterResolver).getParameters();
        var result = request.transformRequestParameters(activityDef);
        assertNotNull(result);
    }

    @Test
    void transformRequestParametersR4() {
        var params = org.opencds.cqf.fhir.utility.r4.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part("param", "true"));
        var bundle = new org.hl7.fhir.r4.model.Bundle();
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, null, null, inputParameterResolver)
                .setData(bundle);
        var activityDef = new org.hl7.fhir.r4.model.ActivityDefinition();
        doReturn(params).when(inputParameterResolver).getParameters();
        var result = request.transformRequestParameters(activityDef);
        assertNotNull(result);
    }

    @Test
    void transformRequestParametersR5() {
        var params = org.opencds.cqf.fhir.utility.r5.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part("param", "true"));
        var bundle = new org.hl7.fhir.r5.model.Bundle();
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R5, libraryEngine, null, null, inputParameterResolver)
                .setData(bundle);
        var activityDef = new org.hl7.fhir.r5.model.ActivityDefinition();
        doReturn(params).when(inputParameterResolver).getParameters();
        var result = request.transformRequestParameters(activityDef);
        assertNotNull(result);
    }

    @Test
    void toPopulateRequestWithLaunchContextParameters() {
        var fhirVersion = FhirVersionEnum.R4;
        var deviceRequest = new org.hl7.fhir.r4.model.DeviceRequest();
        deviceRequest.setId("12345");
        var params = org.opencds.cqf.fhir.utility.r4.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part("DeviceRequest", deviceRequest));
        var questionnaire = new org.hl7.fhir.r4.model.Questionnaire();
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setId("patientId");
        var patientContext = questionnaire.addExtension();
        patientContext.setUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        patientContext.addExtension("name", new org.hl7.fhir.r4.model.Coding("sdc", "patient", "Patient"));
        patientContext.addExtension("type", new org.hl7.fhir.r4.model.CodeType("Patient"));
        var deviceRequestContext = questionnaire.addExtension();
        deviceRequestContext.setUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        deviceRequestContext.addExtension(
                "name", new org.hl7.fhir.r4.model.Coding("sdc", "DeviceRequest", "DeviceRequest"));
        deviceRequestContext.addExtension("type", new org.hl7.fhir.r4.model.CodeType("DeviceRequest"));
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forCached(fhirVersion)).when(repository).fhirContext();
        doReturn(patient).when(repository).read(any(), eq(new IdType("Patient/patientId")));
        // doReturn(deviceRequest).when(repository).read(any(), eq(new IdType("DeviceRequest/12345")));
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        FhirVersionEnum.R4, libraryEngine, null, params, inputParameterResolver)
                .setQuestionnaire(questionnaire);
        var populateRequest = request.toPopulateRequest();
        assertInstanceOf(PopulateRequest.class, populateRequest);
        var parameters = (org.hl7.fhir.r4.model.Parameters) populateRequest.getParameters();
        assertEquals(5, parameters.getParameter().size());
    }
}
