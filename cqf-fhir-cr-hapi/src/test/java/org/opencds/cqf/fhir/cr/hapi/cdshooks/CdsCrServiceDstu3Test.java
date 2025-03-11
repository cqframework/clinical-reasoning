package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class CdsCrServiceDstu3Test extends BaseCdsCrServiceTest {

    private CdsCrServiceDstu3 testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR4Cached();
        repository = new InMemoryFhirRepository(fhirContext);
        cdsConfigService = getCdsConfigService();
        testSubject = new CdsCrServiceDstu3(REQUEST_DETAILS, repository, cdsConfigService);
    }

    @Test
    void fhirVersion() {
        assertEquals(FhirVersionEnum.DSTU3, testSubject.getFhirVersion());
    }

}
