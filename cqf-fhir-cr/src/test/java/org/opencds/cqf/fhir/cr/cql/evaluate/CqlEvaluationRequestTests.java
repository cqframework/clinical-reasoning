package org.opencds.cqf.fhir.cr.cql.evaluate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opencds.cqf.fhir.utility.Parameters.newCanonicalPart;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.BundleHelper;

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
    void testMissingExpressionAndContent() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        assertThrows(
                IllegalArgumentException.class,
                () -> new CqlEvaluationRequest(null, null, null, null, null, null, null, libraryEngine, null));
    }

    @Test
    void testPrefetchDataWithNoData() {
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = new CqlEvaluationRequest(
                null, "1+1", null, null, null, getPrefetchData(FhirContext.forR4Cached()), null, libraryEngine, null);
        assertEquals(2, BundleHelper.getEntry(request.getData()).size());
    }

    @Test
    void testPrefetchDataWithData() {
        var data = new Bundle()
                .setType(BundleType.COLLECTION)
                .addEntry(new BundleEntryComponent().setResource(new Observation()));
        doReturn(repository).when(libraryEngine).getRepository();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        var request = new CqlEvaluationRequest(
                null, "1+1", null, null, data, getPrefetchData(FhirContext.forR4Cached()), null, libraryEngine, null);
        assertEquals(3, BundleHelper.getEntry(request.getData()).size());
    }

    @Test
    void testResolveIncludedLibraries() {
        var request = mock(CqlEvaluationRequest.class, CALLS_REAL_METHODS);
        doReturn(FhirVersionEnum.R4).when(request).getFhirVersion();
        var actual = request.resolveIncludedLibraries(getLibraries(FhirContext.forR4Cached()));
        assertEquals(2, actual.size());
    }

    private List<? extends IBaseBackboneElement> getPrefetchData(FhirContext fhirContext) {
        return List.of(
                (IBaseBackboneElement) newPart(
                        fhirContext,
                        "prefetchData",
                        newStringPart(fhirContext, "key", "patient"),
                        newPart(
                                fhirContext,
                                "data",
                                new Bundle()
                                        .setType(BundleType.COLLECTION)
                                        .addEntry(new BundleEntryComponent()
                                                .setResource(new Patient().setId("patient1"))))),
                (IBaseBackboneElement) newPart(
                        fhirContext,
                        "prefetchData",
                        newStringPart(fhirContext, "key", "observations"),
                        newPart(
                                fhirContext,
                                "data",
                                new Bundle()
                                        .setType(BundleType.COLLECTION)
                                        .addEntry(new BundleEntryComponent()
                                                .setResource(new Observation()
                                                        .setSubject(new Reference("Patient/patient1"))
                                                        .setId("observation1"))))));
    }

    private List<? extends IBaseBackboneElement> getLibraries(FhirContext fhirContext) {
        return List.of(
                (IBaseBackboneElement) newPart(
                        fhirContext,
                        "library",
                        newCanonicalPart(fhirContext, "url", "http://test.org/fhir/Library/Test1"),
                        newStringPart(fhirContext, "name", "Test1")),
                (IBaseBackboneElement) newPart(
                        fhirContext,
                        "library",
                        newCanonicalPart(fhirContext, "url", "http://test.org/fhir/Library/Test2"),
                        newStringPart(fhirContext, "name", "Test2")));
    }
}
