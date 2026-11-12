package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
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
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;

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
                        FhirVersionEnum.DSTU3, libraryEngine, null, inputParameterResolver)
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
                        FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver)
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
                        FhirVersionEnum.R5, libraryEngine, null, inputParameterResolver)
                .setData(bundle);
        var activityDef = new org.hl7.fhir.r5.model.ActivityDefinition();
        doReturn(params).when(inputParameterResolver).getParameters();
        var result = request.transformRequestParameters(activityDef);
        assertNotNull(result);
    }

    @Test
    void toPopulateReturnsNullWithNoQuestionnaire() {
        var request = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.R4, libraryEngine, null, inputParameterResolver);
        var populateRequest = request.toPopulateRequest();
        assertNull(populateRequest);
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
        var userContext = questionnaire.addExtension();
        userContext.setUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        userContext.addExtension("name", new org.hl7.fhir.r4.model.Coding("sdc", "user", "User"));
        userContext.addExtension("type", new org.hl7.fhir.r4.model.CodeType("Practitioner"));
        var encounterContext = questionnaire.addExtension();
        encounterContext.setUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        encounterContext.addExtension("name", new org.hl7.fhir.r4.model.Coding("sdc", "encounter", "Encounter"));
        encounterContext.addExtension("type", new org.hl7.fhir.r4.model.CodeType("Encounter"));
        var deviceRequestContext = questionnaire.addExtension();
        deviceRequestContext.setUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        deviceRequestContext.addExtension(
                "name", new org.hl7.fhir.r4.model.Coding("sdc", "DeviceRequest", "DeviceRequest"));
        deviceRequestContext.addExtension("type", new org.hl7.fhir.r4.model.CodeType("DeviceRequest"));
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forCached(fhirVersion)).when(repository).fhirContext();
        doReturn(patient).when(repository).read(any(), eq(new IdType("Patient/patientId")));
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        fhirVersion, libraryEngine, params, inputParameterResolver)
                .setQuestionnaire(questionnaire);
        var populateRequest = request.toPopulateRequest();
        assertInstanceOf(PopulateRequest.class, populateRequest);
        var parameters = (org.hl7.fhir.r4.model.Parameters) populateRequest.getParameters();
        assertEquals(5, parameters.getParameter().size());
    }

    @Test
    void childItemDefinitionsAreTrackedAndNotDuplicatedWhenAddingNewQuestionnaireItems() {
        var exampleDefinition1 = "http://example.com/fhir/StructureDefinition/example-observation1";
        var exampleDefinition2 = "http://example.com/fhir/StructureDefinition/example-sbservation2";
        var exampleDefinition3 = "http://example.com/fhir/StructureDefinition/example-observation3";
        var fhirVersion = FhirVersionEnum.R4;
        var questionnaire = new org.hl7.fhir.r4.model.Questionnaire();
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        fhirVersion, libraryEngine, null, inputParameterResolver)
                .setQuestionnaire(questionnaire);
        var item1 = newQuestionnaireItem(fhirVersion)
                .setLinkId("1")
                .setType("group")
                .setDefinition(exampleDefinition1);
        item1.setItem(List.of(
                newQuestionnaireItem(fhirVersion)
                        .setLinkId("1.1")
                        .setType("boolean")
                        .setDefinition(String.format("%s#value[x]", exampleDefinition1)),
                newQuestionnaireItem(fhirVersion)
                        .setLinkId("1.2")
                        .setType("date")
                        .setDefinition(String.format("%s#effectiveDate", exampleDefinition1))));
        request.addQuestionnaireItem(item1);
        assertEquals(
                1, request.getQuestionnaireAdapter().getAllItemDefinitions().size());
        var item2 = newQuestionnaireItem(fhirVersion).setLinkId("2").setType("group");
        item2.setItem(List.of(
                newQuestionnaireItem(fhirVersion)
                        .setLinkId("2.1")
                        .setType("boolean")
                        .setDefinition(String.format("%s#value", exampleDefinition2)),
                newQuestionnaireItem(fhirVersion)
                        .setLinkId("2.2")
                        .setType("boolean")
                        .setDefinition(String.format("%s#value", exampleDefinition3))));
        request.addQuestionnaireItem(item2);
        assertEquals(
                3, request.getQuestionnaireAdapter().getAllItemDefinitions().size());
        assertTrue(request.getQuestionnaireAdapter()
                .getAllItemDefinitions()
                .containsAll(List.of(exampleDefinition1, exampleDefinition2, exampleDefinition3)));
    }

    private IQuestionnaireItemComponentAdapter newQuestionnaireItem(FhirVersionEnum fhirVersion) {
        return ((IQuestionnaireItemComponentAdapter)
                IAdapterFactory.createAdapterForBase(fhirVersion, new QuestionnaireItemComponent()));
    }
}
