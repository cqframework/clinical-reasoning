package org.opencds.cqf.fhir.cr.library.evaluate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
public class EvaluateProcessorTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Mock
    private IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    private final EvaluateProcessor fixture = new EvaluateProcessor(repository, EvaluationSettings.getDefault());

    @Test
    void testCanonicalGetsVersion() {
        var subject = "test";
        var name = "TestLibrary";
        var url = "http://example.org/fhir/Library/TestLibrary";
        var version = "1.0.0";
        var library = new Library().setName(name).setUrl(url).setVersion(version);
        library.setId(name);
        var expected = newParameters(fhirContextR4, "results");
        var expectedCanonical = String.format("%s|%s", url, version);
        var request = new EvaluateRequest(
                library, new IdType("Patient", subject), null, null, null, null, libraryEngine, null);
        doReturn(expected)
                .when(libraryEngine)
                .evaluate(
                        eq(expectedCanonical),
                        eq(String.format("Patient/%s", subject)),
                        eq(null),
                        any(),
                        eq(null),
                        eq(null),
                        eq(null));
        var actual = fixture.evaluate(request);
        assertNotNull(actual);
    }
}
