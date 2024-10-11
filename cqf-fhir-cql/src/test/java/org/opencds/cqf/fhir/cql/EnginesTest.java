package org.opencds.cqf.fhir.cql;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;

class EnginesTest {

    @Test
    void debugLoggingEnabled() {
        var repository = mock(Repository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        var settings = EvaluationSettings.getDefault();
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(true);
        var engine = Engines.forRepository(repository, settings, null);
        assertTrue(engine.getState().getDebugMap().getIsLoggingEnabled());
    }

    @Test
    void debugLoggingDisabled() {
        var repository = mock(Repository.class);
        when(repository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        var settings = EvaluationSettings.getDefault();
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(false);
        var engine = Engines.forRepository(repository, settings, null);
        assertNull(engine.getState().getDebugMap());
    }
}
