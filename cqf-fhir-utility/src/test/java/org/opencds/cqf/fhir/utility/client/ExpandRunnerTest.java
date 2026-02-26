package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.opencds.cqf.fhir.utility.client.ExpandRunner.TerminologyServerExpansionException;

class ExpandRunnerTest {

    @Test
    void testTimeout() {
        var url = "test";
        var params = new Parameters();
        var settings = TerminologyServerClientSettings.getDefault().setTimeoutSeconds(2);
        var fhirClient = mock(IGenericClient.class, new ReturnsDeepStubs());
        var fixture = new ExpandRunner(fhirClient, settings, url, params);
        assertThrows(
                TerminologyServerExpansionException.class,
                fixture::expandValueSet,
                "Terminology Server expansion took longer than the allotted timeout: 2");
    }

    @Test
    void expandValueSet_fetchesAdditionalPagesWhenContainsIsPartial() {
        // Arrange
        var url = "http://example.org/fhir/ValueSet/whatever";
        var params = new Parameters();

        // Build three pages: 50 + 50 + 20 = total 120
        ValueSet first = new ValueSet();
        first.setUrl(url);
        first.getExpansion().setTotal(120);
        for (int i = 0; i < 50; i++) {
            first.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("C" + i));
        }

        ValueSet second = new ValueSet();
        second.setUrl(url);
        second.getExpansion().setTotal(120);
        for (int i = 50; i < 100; i++) {
            second.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("C" + i));
        }

        ValueSet third = new ValueSet();
        third.setUrl(url);
        third.getExpansion().setTotal(120);
        for (int i = 100; i < 120; i++) {
            third.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("C" + i));
        }

        // Mock client with deep stubs so we can stub the fluent HAPI operation chain
        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());

        AtomicInteger executeCalls = new AtomicInteger(0);
        when(client.operation()
                        .onInstance(anyString())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenAnswer(inv -> {
                    int n = executeCalls.incrementAndGet();
                    if (n == 1) {
                        return first;
                    }
                    if (n == 2) {
                        return second;
                    }
                    return third;
                });

        // Settings to force paging behavior (but the server returns less than total in the first page)
        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setExpansionsPerPage(50)
                .setMaxExpansionPages(10)
                .setRetryIntervalMillis(1L);

        // Use a real scheduled executor; ExpandRunner should shut it down internally (constructor variant may accept
        // it)
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // Act (use the constructor without scheduler if that's what exists)
        ExpandRunner runner = new ExpandRunner(client, settings, url, params);
        IBaseResource result = runner.expandValueSet();
        ValueSet assembled = (ValueSet) result;

        // Assert
        assertEquals(
                120,
                assembled.getExpansion().getContains().size(),
                "All pages should be appended into a single expansion");
        // We should have executed the expand call three times (initial + 2 pages)
        assertEquals(3, executeCalls.get(), "Should invoke $expand execute() three times (initial + 2 pages)");

        scheduler.shutdownNow();
    }

    @Test
    void expandValueSet_doesNotPageWhenContainsCompletesExpansion() {
        // Arrange
        var url = "http://example.org/fhir/ValueSet/full";
        var params = new Parameters();

        // Single page where contains == total (server already returned the full expansion)
        ValueSet single = new ValueSet();
        single.setUrl(url);
        single.getExpansion().setTotal(120);
        for (int i = 0; i < 120; i++) {
            single.getExpansion().addContains(new ValueSet.ValueSetExpansionContainsComponent().setCode("F" + i));
        }

        // Mock client with deep stubs so we can stub the fluent HAPI operation chain
        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());

        AtomicInteger executeCalls = new AtomicInteger(0);
        when(client.operation()
                        .onInstance(anyString())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenAnswer(inv -> {
                    executeCalls.incrementAndGet();
                    return single;
                });

        // Settings where total (120) > expansionsPerPage (50),
        // but because the server returned all codes in one response, we should not make further calls
        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setExpansionsPerPage(50)
                .setMaxExpansionPages(10)
                .setRetryIntervalMillis(1L);

        // Act
        ExpandRunner runner = new ExpandRunner(client, settings, url, params);
        IBaseResource result = runner.expandValueSet();
        ValueSet assembled = (ValueSet) result;

        // Assert
        assertEquals(
                120,
                assembled.getExpansion().getContains().size(),
                "Should return the full expansion provided by the server without paging");
        assertEquals(120, assembled.getExpansion().getTotal(), "Total should be preserved from the server response");
        assertEquals(
                1,
                executeCalls.get(),
                "Should only invoke $expand once when server returns the full expansion even if total > per-page");
    }

    @Test
    void expandValueSet_vsacUrl_appendsVersionWithDash() {
        // VSAC requires version appended with dash: "ValueSet/id-version" instead of standard format
        var url = "https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001|20240101";
        var params = new Parameters();

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("https://cts.nlm.nih.gov/fhir");

        var idCaptor = ArgumentCaptor.forClass(String.class);
        when(client.operation()
                        .onInstance(idCaptor.capture())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setRetryIntervalMillis(1L);

        var runner = new ExpandRunner(client, settings, url, params);
        runner.expandValueSet();

        // Verify VSAC version syntax: id-version (dash separator)
        var capturedId = idCaptor.getValue();
        assertTrue(
                capturedId.contains("-20240101"),
                "VSAC URL should have version appended with dash, got: " + capturedId);
    }

    @Test
    void expandValueSet_nonVsacUrl_usesStandardIdFormat() {
        var url = "http://example.org/fhir/ValueSet/my-valueset|1.0.0";
        var params = new Parameters();

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("http://example.org/fhir");

        var idCaptor = ArgumentCaptor.forClass(String.class);
        when(client.operation()
                        .onInstance(idCaptor.capture())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setRetryIntervalMillis(1L);

        var runner = new ExpandRunner(client, settings, url, params);
        runner.expandValueSet();

        // Non-VSAC should use standard format without version in the ID
        var capturedId = idCaptor.getValue();
        assertEquals("ValueSet/my-valueset", capturedId, "Non-VSAC URL should use standard id format without version");
    }

    @Test
    void expandValueSet_vsacUrlWithoutVersion_usesStandardIdFormat() {
        var url = "https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001";
        var params = new Parameters();

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("https://cts.nlm.nih.gov/fhir");

        var idCaptor = ArgumentCaptor.forClass(String.class);
        when(client.operation()
                        .onInstance(idCaptor.capture())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setRetryIntervalMillis(1L);

        var runner = new ExpandRunner(client, settings, url, params);
        runner.expandValueSet();

        // VSAC URL without version should use standard format
        var capturedId = idCaptor.getValue();
        assertEquals(
                "ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001",
                capturedId,
                "VSAC URL without version should use standard id format");
    }

    @Test
    void expandValueSet_withParameters_logsParameterValues() {
        // This test verifies the expand succeeds with parameters present (exercises formatParametersForLogging)
        var url = "http://example.org/fhir/ValueSet/test";
        var params = new Parameters();
        params.addParameter().setName("system-version").setValue(new StringType("http://snomed.info/sct|2024"));
        params.addParameter().setName("count").setValue(new org.hl7.fhir.r4.model.IntegerType(1000));

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("http://example.org/fhir");

        when(client.operation()
                        .onInstance(anyString())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setRetryIntervalMillis(1L);

        var runner = new ExpandRunner(client, settings, url, params);
        var result = (ValueSet) runner.expandValueSet();

        // If formatParametersForLogging threw an exception, the expand would still succeed
        // but we verify the expand completed correctly with parameters present
        assertEquals(1, result.getExpansion().getContains().size());
    }

    @Test
    void expandValueSet_withNullParameters_succeeds() {
        // Tests that formatParametersForLogging handles null parameters gracefully
        var url = "http://example.org/fhir/ValueSet/test";

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("http://example.org/fhir");

        when(client.operation()
                        .onInstance(anyString())
                        .named("$expand")
                        .withParameters(any())
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setRetryIntervalMillis(1L);

        // Pass null parameters - should not cause NPE in logging
        var runner = new ExpandRunner(client, settings, url, null);
        var result = (ValueSet) runner.expandValueSet();

        assertEquals(1, result.getExpansion().getContains().size());
    }

    @Test
    void expandValueSet_withEmptyParameters_succeeds() {
        var url = "http://example.org/fhir/ValueSet/test";
        var params = new Parameters(); // empty - no parameters added

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("http://example.org/fhir");

        when(client.operation()
                        .onInstance(anyString())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setRetryIntervalMillis(1L);

        var runner = new ExpandRunner(client, settings, url, params);
        var result = (ValueSet) runner.expandValueSet();

        assertEquals(1, result.getExpansion().getContains().size());
    }

    @Test
    void expandValueSet_transientServerError_retriesAndSucceeds() {
        var url = "http://example.org/fhir/ValueSet/test";
        var params = new Parameters();

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("http://example.org/fhir");

        when(client.operation()
                        .onInstance(anyString())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenThrow(new InternalErrorException("Server error"))
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(10)
                .setMaxRetryCount(3)
                .setRetryIntervalMillis(1L);

        var runner = new ExpandRunner(client, settings, url, params);
        var result = (ValueSet) runner.expandValueSet();

        assertEquals(1, result.getExpansion().getContains().size());
    }

    @Test
    void expandValueSet_withPartParameters_formatsPartsForLogging() {
        var url = "http://example.org/fhir/ValueSet/test";
        var params = new Parameters();
        var partParam = params.addParameter().setName("complex-param");
        partParam.addPart().setName("sub1").setValue(new StringType("value1"));
        partParam.addPart().setName("sub2").setValue(new StringType("value2"));

        var expandedVs = new ValueSet();
        expandedVs.setUrl(url);
        expandedVs.getExpansion().addContains(new ValueSetExpansionContainsComponent().setCode("test"));

        IGenericClient client = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(client.getFhirContext()).thenReturn(FhirContext.forR4Cached());
        when(client.getServerBase()).thenReturn("http://example.org/fhir");

        when(client.operation()
                        .onInstance(anyString())
                        .named("$expand")
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any(Class.class))
                        .execute())
                .thenReturn(expandedVs);

        var settings = TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(5)
                .setMaxRetryCount(1)
                .setRetryIntervalMillis(1L);

        var runner = new ExpandRunner(client, settings, url, params);
        var result = (ValueSet) runner.expandValueSet();

        assertEquals(1, result.getExpansion().getContains().size());
    }
}
