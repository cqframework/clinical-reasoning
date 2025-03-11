package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdsCrServiceR4Test extends BaseCdsCrServiceTest {

    private CdsCrServiceR4 testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR4Cached();
        repository = new InMemoryFhirRepository(fhirContext);
        cdsConfigService = getCdsConfigService();
        testSubject = new CdsCrServiceR4(REQUEST_DETAILS, repository, cdsConfigService);
    }

    @Test
    void fhirVersion() {
        assertEquals(FhirVersionEnum.R4, testSubject.getFhirVersion());
    }
}
