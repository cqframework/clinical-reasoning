package org.opencds.cqf.fhir.cql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.cqframework.fhir.utilities.LoggerAdapter;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EnginesTest {

    private static Logger log = LoggerFactory.getLogger(EnginesTest.class);

    @Test
    void debugLoggingEnabled() {
        var repository = mock(Repository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        var settings = EvaluationSettings.getDefault();
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(true);
        var engine = Engines.forRepository(repository, settings);
        assertTrue(engine.getState().getDebugMap().getIsLoggingEnabled());
    }

    @Test
    void debugLoggingDisabled() {
        var repository = mock(Repository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        var settings = EvaluationSettings.getDefault();
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(false);
        var engine = Engines.forRepository(repository, settings);
        assertNull(engine.getState().getDebugMap());
    }

    @Test
    void npmProcessor() {
        var repository = mock(Repository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());

        var igContext = new IGContext(new LoggerAdapter(log));
        igContext.initializeFromIni("todo.ini");
        var settings = EvaluationSettings
                .getDefault()
                .toBuilder()
                .npmProcessor(new NpmProcessor(igContext))
                .build();

        var engine = Engines.forRepository(repository, settings);
        var lm = engine.getEnvironment().getLibraryManager();

        var ni = lm.getNamespaceManager().getNamespaceInfoFromUri("");
        assertNotNull(ni);
        assertEquals("FHIR", ni.getName());
    }
}
