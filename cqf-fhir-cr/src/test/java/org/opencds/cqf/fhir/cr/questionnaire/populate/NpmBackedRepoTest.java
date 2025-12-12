package org.opencds.cqf.fhir.cr.questionnaire.populate;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.repository.INpmRepository;

@ExtendWith(MockitoExtension.class)
public class NpmBackedRepoTest {

    @Mock
    private IRepository repository;

    @Mock
    private INpmRepository npmRepository;

    @Spy
    private FhirContext fhirCtx = FhirContext.forR4Cached();

    @InjectMocks
    private NpmBackedRepository npmBackedRepo;

    @Test
    public void search_forAppropriateResources_works() {
        // setup

    }
}
