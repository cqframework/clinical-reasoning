package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TerminologyServerClientSettingsTest {

    @Test
    void testSettings() {
        var retryCount = 5;
        var interval = 500;
        var timeout = 10;
        var settings = new TerminologyServerClientSettings()
                .setMaxRetryCount(retryCount)
                .setRetryIntervalMillis(interval)
                .setTimeoutSeconds(timeout);
        assertEquals(retryCount, settings.getMaxRetryCount());
        assertEquals(interval, settings.getRetryIntervalMillis());
        assertEquals(timeout, settings.getTimeoutSeconds());
        var copy = new TerminologyServerClientSettings(settings);
        assertEquals(settings.getMaxRetryCount(), copy.getMaxRetryCount());
        assertEquals(settings.getRetryIntervalMillis(), copy.getRetryIntervalMillis());
        assertEquals(settings.getTimeoutSeconds(), copy.getTimeoutSeconds());
    }
}
