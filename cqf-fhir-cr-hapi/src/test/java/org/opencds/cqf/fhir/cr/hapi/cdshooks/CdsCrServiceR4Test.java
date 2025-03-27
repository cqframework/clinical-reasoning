package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.hapi.config.CrCdsHooksConfig.PLAN_DEFINITION_RESOURCE_NAME;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import ca.uhn.hapi.fhir.cdshooks.module.CdsHooksObjectMapperFactory;
import java.io.IOException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class CdsCrServiceR4Test extends BaseCdsCrServiceTest {

    private CdsCrService testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR4Cached();
        repository = new InMemoryFhirRepository(fhirContext);
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        objectMapper = new CdsHooksObjectMapperFactory(fhirContext).newMapper();
        cdsConfigService = getCdsConfigService();
        testSubject = new CdsCrService(REQUEST_DETAILS, repository, cdsConfigService);
    }

    @Test
    void fhirVersion() {
        assertEquals(FhirVersionEnum.R4, testSubject.getFhirVersion());
    }

    @Test
    void testGetRepository() {
        assertEquals(repository, testSubject.getRepository());
    }

    @Test
    public void testR4Params() throws IOException {
        final String rawRequest =
                ClasspathUtil.loadResource("org/opencds/cqf/fhir/cr/hapi/cdshooks/ASLPCrdServiceRequest.json");
        final CdsServiceRequestJson cdsServiceRequestJson =
                objectMapper.readValue(rawRequest, CdsServiceRequestJson.class);
        final Bundle bundle = ClasspathUtil.loadResource(
                fhirContext, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/Bundle-ASLPCrd-Content.json");
        final Repository repository = new InMemoryFhirRepository(fhirContext, bundle);
        final RequestDetails requestDetails = new SystemRequestDetails();
        final IdType planDefinitionId = new IdType(PLAN_DEFINITION_RESOURCE_NAME, "ASLPCrd");
        requestDetails.setId(planDefinitionId);
        final var params = adapterFactory.createParameters(
                new CdsCrService(requestDetails, repository, cdsConfigService).encodeParams(cdsServiceRequestJson));

        assertEquals(2, params.getParameter().size());
        assertTrue((params.getParameter("parameters")).hasResource());
    }

    @Test
    public void testR4Response() {
        final Bundle bundle = ClasspathUtil.loadResource(
                fhirContext, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/Bundle-ASLPCrd-Content.json");
        final Repository repository = new InMemoryFhirRepository(fhirContext, bundle);
        final Bundle responseBundle = ClasspathUtil.loadResource(
                fhirContext, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/cdshooks/Bundle-ASLPCrd-Response.json");
        final Parameters response = new Parameters()
                .addParameter(
                        new ParametersParameterComponent().setName("return").setResource(responseBundle));
        final RequestDetails requestDetails = new SystemRequestDetails();
        final IdType planDefinitionId = new IdType(PLAN_DEFINITION_RESOURCE_NAME, "ASLPCrd");
        requestDetails.setId(planDefinitionId);
        final CdsServiceResponseJson cdsServiceResponseJson =
                new CdsCrService(requestDetails, repository, cdsConfigService).encodeResponse(response);

        assertEquals(1, cdsServiceResponseJson.getCards().size());
        assertFalse(cdsServiceResponseJson.getCards().get(0).getSummary().isEmpty());
        assertFalse(cdsServiceResponseJson.getCards().get(0).getDetail().isEmpty());
    }
}
