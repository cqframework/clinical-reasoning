package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;

@ExtendWith(MockitoExtension.class)
class ProcessItemTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Mock
    private Repository repository;

    @Mock
    ExpressionProcessor expressionProcessor;

    @Mock
    private LibraryEngine libraryEngine;

    private ProcessItem fixture;

    @BeforeEach
    void setup() {
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(repository).when(libraryEngine).getRepository();
        fixture = new ProcessItem();
    }
}
