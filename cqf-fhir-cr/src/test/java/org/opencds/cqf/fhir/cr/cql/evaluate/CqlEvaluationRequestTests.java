package org.opencds.cqf.fhir.cr.cql.evaluate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class CqlEvaluationRequestTests {

    @Mock
    private IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Test
    void test() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = new CqlEvaluationRequest(null, "1+1", null, null, null, null, null, libraryEngine, null);
        assertEquals("cql", request.getOperationName());
        assertNotNull(request.getModelResolver());
    }

    @Test
    void testResolveIncludedLibraries() {
        var request = mock(CqlEvaluationRequest.class, CALLS_REAL_METHODS);
        var actual = request.resolveIncludedLibraries(null);
        assertNull(actual);
    }
}
