package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CdsCrSettingsTest {

    @Test
    @DisplayName("getDefault() should return settings with the default client ID header name")
    void getDefault_shouldReturnDefaultSettings() {
        // Act
        CdsCrSettings settings = CdsCrSettings.getDefault();

        // Assert
        assertNotNull(settings);
        assertEquals("client_id", settings.getClientIdHeaderName());
    }

    @Test
    @DisplayName("Default constructor should initialize clientIdHeaderName as null")
    void constructor_shouldInitializeHeaderNameToNull() {
        // Act
        CdsCrSettings settings = new CdsCrSettings();

        // Assert
        assertNull(settings.getClientIdHeaderName());
    }

    @Test
    @DisplayName("setClientIdHeaderName() should correctly update the value")
    void setClientIdHeaderName_shouldUpdateValue() {
        // Arrange
        CdsCrSettings settings = new CdsCrSettings();
        String customHeaderName = "X-Custom-Client-ID";

        // Act
        settings.setClientIdHeaderName(customHeaderName);

        // Assert
        assertEquals(customHeaderName, settings.getClientIdHeaderName());
    }

    @Test
    @DisplayName("setClientIdHeaderName() should allow setting the value to null")
    void setClientIdHeaderName_shouldAllowNull() {
        // Arrange
        CdsCrSettings settings = CdsCrSettings.getDefault(); // Start with a non-null value

        // Act
        settings.setClientIdHeaderName(null);

        // Assert
        assertNull(settings.getClientIdHeaderName());
    }

    @Test
    @DisplayName("withClientIdHeaderName() should update value and return the same instance")
    void withClientIdHeaderName_shouldBeFluent() {
        // Arrange
        CdsCrSettings settings = new CdsCrSettings();
        String customHeaderName = "X-Fluent-Client-ID";

        // Act
        CdsCrSettings returnedSettings = settings.withClientIdHeaderName(customHeaderName);

        // Assert
        assertEquals(customHeaderName, settings.getClientIdHeaderName());
        assertSame(settings, returnedSettings);
    }
}
