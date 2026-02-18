package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GenericGenericTerminologyServerClientClientSettingsTest {

    @Test
    void testSettings() {
        var retryCount = 5;
        var interval = 500;
        var timeout = 10;
        var socketTimeout = 45;
        var expansionsPerPage = 500;
        var maxExpansionPages = 500;
        var settings = TerminologyServerClientSettings.getDefault()
                .setMaxRetryCount(retryCount)
                .setRetryIntervalMillis(interval)
                .setTimeoutSeconds(timeout)
                .setSocketTimeout(socketTimeout)
                .setCrmiVersion("2.0.0")
                .setExpansionsPerPage(expansionsPerPage)
                .setMaxExpansionPages(maxExpansionPages);
        assertEquals(retryCount, settings.getMaxRetryCount());
        assertEquals(interval, settings.getRetryIntervalMillis());
        assertEquals(timeout, settings.getTimeoutSeconds());
        assertEquals(socketTimeout, settings.getSocketTimeout());
        assertEquals(expansionsPerPage, settings.getExpansionsPerPage());
        assertEquals(maxExpansionPages, settings.getMaxExpansionPages());
        var copy = new TerminologyServerClientSettings(settings);
        assertEquals(settings.getMaxRetryCount(), copy.getMaxRetryCount());
        assertEquals(settings.getRetryIntervalMillis(), copy.getRetryIntervalMillis());
        assertEquals(settings.getTimeoutSeconds(), copy.getTimeoutSeconds());
        assertEquals(settings.getSocketTimeout(), copy.getSocketTimeout());
        assertEquals(settings.getCrmiVersion(), copy.getCrmiVersion());
        assertEquals(settings.getExpansionsPerPage(), copy.getExpansionsPerPage());
        assertEquals(settings.getMaxExpansionPages(), copy.getMaxExpansionPages());
    }
}
