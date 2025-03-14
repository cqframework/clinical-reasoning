package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class CdsCrServiceR5Test extends BaseCdsCrServiceTest {

    private CdsCrService testSubject;

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR5Cached();
        repository = new InMemoryFhirRepository(fhirContext);
        cdsConfigService = getCdsConfigService();
        testSubject = new CdsCrService(REQUEST_DETAILS, repository, cdsConfigService);
    }

    @Test
    void fhirVersion() {
        assertEquals(FhirVersionEnum.R5, testSubject.getFhirVersion());
    }

    @Test
    void testGetRepository() {
        assertEquals(repository, testSubject.getRepository());
    }
}
