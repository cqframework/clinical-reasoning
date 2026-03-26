package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ProxyRepository;

/**
 * Unit tests for {@link MeasureEnvironment#resolve(IRepository)}.
 *
 * <p>The critical invariant: a ProxyRepository must be created whenever <em>any</em> endpoint is
 * provided, not only when all three are present. {@code Repositories.proxy()} already handles null
 * per-endpoint entries by falling back to the local repository.
 */
class MeasureEnvironmentTest {

    private static IRepository base;

    // A real Endpoint is needed because Repositories.proxy() dispatches on fhirContext() and
    // passes the endpoint to Clients.forEndpoint(). Client construction is lazy (no network call),
    // so a stub address is safe in unit tests.
    private static Endpoint dataEndpoint;
    private static Endpoint contentEndpoint;
    private static Endpoint terminologyEndpoint;

    @BeforeAll
    static void setup() {
        base = new InMemoryFhirRepository(FhirContext.forR4Cached());
        dataEndpoint = new Endpoint().setAddress("http://data.example.org/fhir");
        contentEndpoint = new Endpoint().setAddress("http://content.example.org/fhir");
        terminologyEndpoint = new Endpoint().setAddress("http://terminology.example.org/fhir");
    }

    // ── no-op cases ──────────────────────────────────────────────────────────

    @Test
    void resolve_emptyEnvironment_returnsBaseUnchanged() {
        IRepository result = MeasureEnvironment.EMPTY.resolve(base);
        assertSame(base, result, "EMPTY environment should return the base repository as-is");
    }

    @Test
    void resolve_allEndpointsNull_noAdditionalData_returnsBaseUnchanged() {
        var env = new MeasureEnvironment(null, null, null, null);
        assertSame(base, env.resolve(base));
    }

    // ── single-endpoint cases (the cases the AND bug broke) ──────────────────

    @Test
    void resolve_onlyDataEndpoint_returnsProxyRepository() {
        var env = new MeasureEnvironment(null, null, dataEndpoint, null);
        assertInstanceOf(ProxyRepository.class, env.resolve(base));
    }

    @Test
    void resolve_onlyContentEndpoint_returnsProxyRepository() {
        var env = new MeasureEnvironment(contentEndpoint, null, null, null);
        assertInstanceOf(ProxyRepository.class, env.resolve(base));
    }

    @Test
    void resolve_onlyTerminologyEndpoint_returnsProxyRepository() {
        var env = new MeasureEnvironment(null, terminologyEndpoint, null, null);
        assertInstanceOf(ProxyRepository.class, env.resolve(base));
    }

    // ── all-endpoints case (worked before, must still work) ──────────────────

    @Test
    void resolve_allEndpoints_returnsProxyRepository() {
        var env = new MeasureEnvironment(contentEndpoint, terminologyEndpoint, dataEndpoint, null);
        assertInstanceOf(ProxyRepository.class, env.resolve(base));
    }

    // ── additionalData only ───────────────────────────────────────────────────

    @Test
    void resolve_onlyAdditionalData_returnsFederatedRepository() {
        Bundle bundle = bundleWithPatient("p1");
        var env = new MeasureEnvironment(null, null, null, bundle);
        assertInstanceOf(FederatedRepository.class, env.resolve(base));
    }

    // ── endpoints + additionalData ────────────────────────────────────────────

    @Test
    void resolve_endpointsAndAdditionalData_returnsFederatedRepository() {
        // FederatedRepository wraps the ProxyRepository; the outer type is FederatedRepository.
        Bundle bundle = bundleWithPatient("p2");
        var env = new MeasureEnvironment(contentEndpoint, terminologyEndpoint, dataEndpoint, bundle);
        assertInstanceOf(FederatedRepository.class, env.resolve(base));
    }

    @Test
    void resolve_singleEndpointAndAdditionalData_returnsFederatedRepository() {
        Bundle bundle = bundleWithPatient("p3");
        var env = new MeasureEnvironment(null, null, dataEndpoint, bundle);
        assertInstanceOf(FederatedRepository.class, env.resolve(base));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Bundle bundleWithPatient(String id) {
        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.addEntry().setResource(new Patient().setId(id));
        return bundle;
    }
}
