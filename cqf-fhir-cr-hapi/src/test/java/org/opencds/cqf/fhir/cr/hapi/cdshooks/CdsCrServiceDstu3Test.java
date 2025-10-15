package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("UnstableApiUsage")
class CdsCrServiceDstu3Test extends BaseCdsCrServiceTest {

    private CdsCrService testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forDstu3Cached();
        repository = getRepository();
        testSubject = new CdsCrService(REQUEST_DETAILS, repository);
    }

    @Test
    void fhirVersion() {
        assertEquals(FhirVersionEnum.DSTU3, testSubject.getFhirVersion());
    }

    @Test
    void testGetRepository() {
        assertEquals(repository, testSubject.getRepository());
    }

    @Test
    void testDstu3Response() {
        // setup
        final var bundle = ClasspathUtil.loadResource(
                fhirContext,
                Bundle.class,
                "org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-view-bundle.json");
        final IRepository localRepository = new InMemoryFhirRepository(fhirContext, bundle);

        var carePlanResponse = getCarePLanAsResponse();

        CdsResponseEncoderService encoder = new CdsResponseEncoderService(localRepository);

        CdsServiceResponseJson cdsServiceResponseJson = encoder.encodeResponse(carePlanResponse);

        assertEquals(1, cdsServiceResponseJson.getCards().size());
        assertFalse(cdsServiceResponseJson.getCards().get(0).getSummary().isEmpty());
        assertFalse(cdsServiceResponseJson.getCards().get(0).getDetail().isEmpty());
    }

    private CarePlan getCarePLanAsResponse() {
        var carePlanResourceLocation = "org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-careplan.json";

        var retVal = ClasspathUtil.loadResource(
                fhirContext, org.hl7.fhir.dstu3.model.CarePlan.class, carePlanResourceLocation);

        //        Method ClasspathUtil.loadResource will invoke the hapi-fhir json parser to parse and inflate file
        // Resources.
        //        There is a bug in the parser where the value of a contained resource's resourceType is ignored during
        // deserialization.
        //        See issue https://github.com/hapifhir/hapi-fhir/issues/7289
        //
        //        Until the issue is resolved, we set the value for resourceType manually.

        var resource = retVal.getContained().get(0);
        String incompleteId = resource.getId();
        resource.setId(new IdType("RequestGroup/" + incompleteId));

        return retVal;
    }

    public FhirVersionEnum getFhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }
}
