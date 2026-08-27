package org.opencds.cqf.fhir.cr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.IResourceValidator;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

class CrSettingsTest {

    @Test
    void defaultConstructorHasNullValidator() {
        CrSettings settings = new CrSettings();

        assertNull(settings.getResourceValidator());
    }

    @Test
    void getDefaultReturnsNewInstance() {
        CrSettings settings = CrSettings.getDefault();

        assertNotNull(settings);
        assertNotNull(settings.getEvaluationSettings());
        assertNotNull(settings.getTerminologyServerClientSettings());
        assertNull(settings.getResourceValidator());
    }

    @Test
    void withResourceValidatorSetAndReturnsThis() {
        CrSettings settings = new CrSettings();
        IResourceValidator mockValidator = mock(IResourceValidator.class);

        CrSettings result = settings.withResourceValidator(mockValidator);

        assertSame(settings, result);
        assertSame(mockValidator, settings.getResourceValidator());
    }

    @Test
    void setResourceValidatorSetsValue() {
        CrSettings settings = new CrSettings();
        IResourceValidator mockValidator = mock(IResourceValidator.class);

        settings.setResourceValidator(mockValidator);

        assertSame(mockValidator, settings.getResourceValidator());
    }

    @Test
    void fluentChainingWorksWithAllSettings() {
        IResourceValidator mockValidator = mock(IResourceValidator.class);
        EvaluationSettings evalSettings = EvaluationSettings.getDefault();
        TerminologyServerClientSettings termSettings = TerminologyServerClientSettings.getDefault();

        CrSettings settings = CrSettings.getDefault()
                .withEvaluationSettings(evalSettings)
                .withTerminologyServerClientSettings(termSettings)
                .withResourceValidator(mockValidator);

        assertSame(evalSettings, settings.getEvaluationSettings());
        assertSame(termSettings, settings.getTerminologyServerClientSettings());
        assertSame(mockValidator, settings.getResourceValidator());
    }
}
