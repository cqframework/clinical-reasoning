package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_DATA;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_ENCOUNTER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_PARAMETERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_DRAFT_ORDERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_ENCOUNTER_ID;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_USER_ID;
import static org.opencds.cqf.fhir.cr.hapi.config.CrCdsHooksConfig.PLAN_DEFINITION_RESOURCE_NAME;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import ca.uhn.hapi.fhir.cdshooks.module.CdsHooksObjectMapperFactory;
import java.io.IOException;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class CdsCrServiceR4Test extends BaseCdsCrServiceTest {

    private CdsCrService testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR4Cached();
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        objectMapper = new CdsHooksObjectMapperFactory(fhirContext).newMapper();
        testSubject = new CdsCrService(
                getRepository(), getAdapterFactory(), getCdsResponseEncoderService(), getCdsParametersEncoderService());
    }

    @Test
    void fhirVersion() {
        assertEquals(FhirVersionEnum.R4, testSubject.getFhirVersion());
    }

    @Test
    void testR4Params() throws IOException {
        final String rawRequest =
                ClasspathUtil.loadResource("org/opencds/cqf/fhir/cr/hapi/cdshooks/ASLPCrdServiceRequest.json");
        final CdsServiceRequestJson cdsServiceRequestJson =
                objectMapper.readValue(rawRequest, CdsServiceRequestJson.class);
        final Bundle bundle = ClasspathUtil.loadResource(
                fhirContext, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/Bundle-ASLPCrd-Content.json");
        final IRepository localRepository = new InMemoryFhirRepository(fhirContext, bundle);
        final RequestDetails localRequestDetails = new SystemRequestDetails();
        final IdType planDefinitionId = new IdType(PLAN_DEFINITION_RESOURCE_NAME, "ASLPCrd");
        localRequestDetails.setId(planDefinitionId);

        CdsCrService cdsCrService = new CdsCrService(
                localRepository, getAdapterFactory(), getCdsResponseEncoderService(), getCdsParametersEncoderService());

        IBaseParameters iBaseParameters = cdsCrService.encodeParams(cdsServiceRequestJson, localRequestDetails);

        final var params = adapterFactory.createParameters(iBaseParameters);

        assertEquals(2, params.getParameter().size());
        assertTrue((params.getParameter("parameters")).hasResource());
    }

    @Test
    void testR4EncodeVariousParams() {
        final String expectedUserId = "user-id";
        final String expectedEncounterId = "encounter-id";
        final String expectedPatientId = "patient-id";

        CdsServiceRequestJson cdsServiceRequestJson = new CdsServiceRequestJson();
        cdsServiceRequestJson.addContext(CDS_PARAMETER_USER_ID, expectedUserId);
        cdsServiceRequestJson.addContext(CDS_PARAMETER_ENCOUNTER_ID, expectedEncounterId);
        cdsServiceRequestJson.addPrefetch("patientKey", new Patient().setId(expectedPatientId));

        IBaseParameters iBaseParameters = testSubject.encodeParams(cdsServiceRequestJson, requestDetails);

        final var params = adapterFactory.createParameters(iBaseParameters);

        assertEquals(3, params.getParameter().size());
        assertEquals(
                (params.getParameter(APPLY_PARAMETER_PRACTITIONER)).getValue().toString(), expectedUserId);
        assertEquals((params.getParameter(APPLY_PARAMETER_ENCOUNTER)).getValue().toString(), expectedEncounterId);

        assertTrue(params.getParameter(APPLY_PARAMETER_DATA).hasResource());
        IBaseResource resource = params.getParameter(APPLY_PARAMETER_DATA).getResource();
        assertInstanceOf(Bundle.class, resource);

        Bundle bundle = (Bundle) resource;
        assertEquals(1, bundle.getEntry().size());
        assertEquals(
                expectedPatientId,
                bundle.getEntryFirstRep().getResource().getIdElement().getIdPart());
    }

    @Test
    void testEncodeCqlParameters() {

        CdsServiceRequestJson cdsServiceRequestJson = new CdsServiceRequestJson();
        cdsServiceRequestJson.addContext(CDS_PARAMETER_DRAFT_ORDERS, new Patient().setId("patient-id"));

        IBaseParameters iBaseParameters = testSubject.encodeParams(cdsServiceRequestJson, requestDetails);

        final var params = adapterFactory.createParameters(iBaseParameters);

        ParametersParameterComponent parametersParameterComponent = ((Parameters)
                        params.getParameter(APPLY_PARAMETER_PARAMETERS).getResource())
                .getParameter()
                .get(0);

        assertEquals(CDS_PARAMETER_DRAFT_ORDERS, parametersParameterComponent.getName());
        assertTrue(parametersParameterComponent.hasResource());
        assertInstanceOf(Patient.class, parametersParameterComponent.getResource());

        Extension extension = parametersParameterComponent.getExtensionFirstRep();
        assertEquals(Constants.CPG_PARAMETER_DEFINITION, extension.getUrl());

        ParameterDefinition parameterDefinition = (ParameterDefinition) extension.getValue();
        assertEquals(CDS_PARAMETER_DRAFT_ORDERS, parameterDefinition.getName());
        assertEquals("*", parameterDefinition.getMax());
    }

    @Test
    void testR4Response() {
        final Bundle bundle = ClasspathUtil.loadResource(
                fhirContext, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/Bundle-ASLPCrd-Content.json");
        final IRepository localRepository = new InMemoryFhirRepository(fhirContext, bundle);
        final Bundle responseBundle = ClasspathUtil.loadResource(
                fhirContext, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/Bundle-ASLPCrd-Response.json");
        final CdsResponseEncoderService encoder = new CdsResponseEncoderService(localRepository, getAdapterFactory());
        final Parameters response = new Parameters()
                .addParameter(
                        new ParametersParameterComponent().setName("return").setResource(responseBundle));

        final IdType planDefinitionId = new IdType(PLAN_DEFINITION_RESOURCE_NAME, "ASLPCrd");
        requestDetails.setId(planDefinitionId);

        CdsCrService cdsCrService =
                new CdsCrService(localRepository, getAdapterFactory(), encoder, getCdsParametersEncoderService());
        final CdsServiceResponseJson cdsServiceResponseJson = cdsCrService.encodeResponse(response, requestDetails);

        assertEquals(1, cdsServiceResponseJson.getCards().size());
        assertFalse(cdsServiceResponseJson.getCards().get(0).getSummary().isEmpty());
        assertFalse(cdsServiceResponseJson.getCards().get(0).getDetail().isEmpty());
    }

    @Test
    void testGetResourcesFromBundle() {
        // Arrange
        final String patientKey = "Patient123";

        Patient patient = new Patient();
        patient.setId(new IdType("Patient", "123"));
        IBaseBundle bundle = BundleHelper.newBundle(FhirVersionEnum.R4);
        BundleHelper.addEntry(bundle, BundleHelper.newEntryWithResource(patient));

        // Act
        Map<String, IBaseResource> resourceMap = cdsParametersEncoderService.getResourcesFromBundle(bundle);

        // Assert
        assertNotNull(resourceMap);
        assertEquals(1, resourceMap.size());
        assertTrue(resourceMap.containsKey(patientKey));
        assertEquals(patient, resourceMap.get(patientKey));
    }
}
