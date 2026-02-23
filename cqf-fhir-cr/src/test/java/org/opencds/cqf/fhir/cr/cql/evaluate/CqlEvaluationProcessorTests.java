package org.opencds.cqf.fhir.cr.cql.evaluate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class CqlEvaluationProcessorTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();
    private final String simpleLibraryName = "simpleTest";
    private final String content = """
            library simpleTest

            using FHIR version '4.0.1'

            context Patient

            define Test:
                5*5
            """;

    @Mock
    private IRepository repository;

    @Spy
    private final CqlEvaluationProcessor fixture = new CqlEvaluationProcessor(repository, evaluationSettings);

    @Test
    void testResolvesNullIdentifier() {
        assertNull(fixture.resolveLibraryIdentifier(null, null, null));
    }

    @Test
    void testResolvesStringContentLibraryIdentifier() {
        doReturn(fhirContextR4).when(repository).fhirContext();
        var engine = Engines.forRepository(repository, evaluationSettings, null);
        var libraryManager = engine.getEnvironment().getLibraryManager();
        var actual = fixture.resolveLibraryIdentifier(content, null, libraryManager);
        assertEquals(simpleLibraryName, actual.getId());
        assertNull(actual.getVersion());
    }

    @Test
    void testResolvesLibraryIdentifier() {
        doReturn(fhirContextR4).when(repository).fhirContext();
        var engine = Engines.forRepository(repository, evaluationSettings, null);
        var libraryManager = engine.getEnvironment().getLibraryManager();
        var version = "1.0.0";
        var library = new Library()
                .setName(simpleLibraryName)
                .setVersion(version)
                .setContent(List.of(new Attachment().setContentType("text/cql").setData(content.getBytes())))
                .setId(simpleLibraryName);
        var actual = fixture.resolveLibraryIdentifier(null, library, libraryManager);
        assertEquals(simpleLibraryName, actual.getId());
        assertEquals(version, actual.getVersion());
    }
}
