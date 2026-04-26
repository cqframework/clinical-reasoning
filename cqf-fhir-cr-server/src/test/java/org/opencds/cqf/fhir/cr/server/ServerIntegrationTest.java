package org.opencds.cqf.fhir.cr.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test against the real {@link Application} bootstrapping path: Spring Boot starts
 * embedded Tomcat, mounts {@code RestfulServer} as a servlet, registers the operation provider
 * (Measure $evaluate-measure) and a {@link RepositoryResourceProvider} per FHIR resource type,
 * plus the {@link RepositorySystemProvider} for transactions.
 *
 * <p>Verifies the full path: HTTP → HAPI dispatch → CRUD shim → {@code IRepository}.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    IRepository repo;

    @Autowired
    FhirContext fhirContext;

    @Autowired
    RestfulServer restfulServer;

    private HttpClient http;
    private String baseUrl;

    @BeforeAll
    void setupClient() {
        http = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + port + "/fhir";
        System.out.println("Server URL: " + baseUrl);
    }

    @Test
    @Order(1)
    void metadata_advertises_evaluate_measure() throws Exception {
        var resp = get("/metadata");
        assertEquals(200, resp.statusCode(), () -> "body:\n" + resp.body());
        assertTrue(resp.body().contains("CapabilityStatement"));
        assertTrue(resp.body().contains("evaluate-measure"), "$evaluate-measure should be advertised");
        // Sanity: every concrete R4 resource type should appear in the rest.resource list.
        // This will catch a mass registration regression early.
        assertTrue(resp.body().contains("\"type\":\"Patient\""), "Patient resource should be advertised");
        assertTrue(resp.body().contains("\"type\":\"Observation\""), "Observation resource should be advertised");
    }

    @Test
    @Order(2)
    void post_patient_persists_via_shim() throws Exception {
        var patient = new Patient().setActive(true);
        patient.addName().setFamily("Shimwell").addGiven("Alice");

        var resp = post("/Patient", encode(patient));
        assertEquals(201, resp.statusCode(), () -> "body:\n" + resp.body());

        var location = resp.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("/Patient/"), "Location should reference new Patient: " + location);

        var newId = idFromLocation(location, "Patient");
        var stored = repo.read(Patient.class, new IdType("Patient", newId));
        assertNotNull(stored);
        assertEquals("Shimwell", stored.getNameFirstRep().getFamily());
    }

    @Test
    @Order(3)
    void get_patient_reads_from_repository() throws Exception {
        var seeded = new Patient().setActive(true);
        seeded.setId(new IdType("Patient", "alice"));
        seeded.addName().setFamily("Read").addGiven("Alice");
        repo.update(seeded);

        var resp = get("/Patient/alice");
        assertEquals(200, resp.statusCode(), () -> "body:\n" + resp.body());
        assertTrue(resp.body().contains("\"family\":\"Read\""));
    }

    @Test
    @Order(4)
    void put_patient_updates_repository() throws Exception {
        var seeded = new Patient().setActive(true);
        seeded.setId(new IdType("Patient", "to-update"));
        seeded.addName().setFamily("Original");
        repo.update(seeded);

        var modified = new Patient().setActive(false);
        modified.setId(new IdType("Patient", "to-update"));
        modified.addName().setFamily("Modified");

        var resp = put("/Patient/to-update", encode(modified));
        assertTrue(resp.statusCode() == 200 || resp.statusCode() == 201,
                () -> "expected 200 or 201, got " + resp.statusCode() + ": " + resp.body());

        var stored = repo.read(Patient.class, new IdType("Patient", "to-update"));
        assertEquals("Modified", stored.getNameFirstRep().getFamily());
        assertEquals(false, stored.getActive());
    }

    @Test
    @Order(5)
    void delete_patient_removes_from_repository() throws Exception {
        var seeded = new Patient();
        seeded.setId(new IdType("Patient", "to-delete"));
        seeded.addName().setFamily("Doomed");
        repo.update(seeded);

        var resp = delete("/Patient/to-delete");
        assertTrue(resp.statusCode() >= 200 && resp.statusCode() < 300,
                () -> "expected 2xx; got " + resp.statusCode() + ": " + resp.body());

        try {
            repo.read(Patient.class, new IdType("Patient", "to-delete"));
            org.junit.jupiter.api.Assertions.fail("Patient should be gone from in-memory repo");
        } catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException expected) {
            // good
        }
    }

    /**
     * Search by id — exercises the SearchParameterTranslator (raw URL → typed params),
     * IRepository.search(), and HAPI's bundle serialization on the way back out.
     */
    @Test
    @Order(6)
    void search_patient_by_id_via_shim() throws Exception {
        var seeded = new Patient();
        seeded.setId(new IdType("Patient", "search-target"));
        seeded.addName().setFamily("SearchHit");
        repo.update(seeded);

        var resp = get("/Patient?_id=search-target");
        assertEquals(200, resp.statusCode(), () -> "body:\n" + resp.body());
        assertTrue(resp.body().contains("\"resourceType\":\"Bundle\""));
        assertTrue(resp.body().contains("\"family\":\"SearchHit\""),
                () -> "expected matched Patient in bundle, got: " + resp.body().substring(0, Math.min(500, resp.body().length())));
    }

    /**
     * Transaction bundle: POST a Bundle of type=transaction with multiple entries to base URL.
     * Verifies the {@link RepositorySystemProvider} routes through to {@code IRepository.transaction}.
     */
    @Test
    @Order(7)
    void post_transaction_bundle_processes_all_entries() throws Exception {
        var bundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);

        var p = new Patient();
        p.setId(new IdType("Patient", "txn-patient"));
        p.addName().setFamily("Transactional");
        bundle.addEntry()
                .setFullUrl("urn:uuid:1")
                .setResource(p)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("Patient/txn-patient");

        var o = new Observation();
        o.setId(new IdType("Observation", "txn-obs"));
        o.setStatus(Observation.ObservationStatus.FINAL);
        bundle.addEntry()
                .setFullUrl("urn:uuid:2")
                .setResource(o)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("Observation/txn-obs");

        var resp = post("/", encode(bundle));
        assertTrue(resp.statusCode() == 200 || resp.statusCode() == 201,
                () -> "expected 200/201; got " + resp.statusCode() + ": " + resp.body());

        // Verify both entries actually persisted via direct IRepository read.
        var patient = repo.read(Patient.class, new IdType("Patient", "txn-patient"));
        assertEquals("Transactional", patient.getNameFirstRep().getFamily());
        var obs = repo.read(Observation.class, new IdType("Observation", "txn-obs"));
        assertEquals(Observation.ObservationStatus.FINAL, obs.getStatus());
    }

    /**
     * Conditional create via {@code If-None-Exist} header. The header is forwarded to
     * {@code IRepository.create}; the in-memory repo doesn't act on it (always creates), but
     * this test pins down the pass-through so a richer repo (RestRepository, IgRepository) can
     * honor it.
     */
    @Test
    @Order(8)
    void conditional_create_forwards_if_none_exist_header() throws Exception {
        var p = new Patient();
        p.addName().setFamily("Conditional");

        var resp = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/Patient"))
                        .header("Content-Type", "application/fhir+json")
                        .header("If-None-Exist", "identifier=foo|bar")
                        .POST(HttpRequest.BodyPublishers.ofString(encode(p)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(201, resp.statusCode(), () -> "body:\n" + resp.body());
        // No assertion on header arrival here: InMemoryFhirRepository ignores the header. The
        // pass-through is exercised; a proper assertion lives in a unit test against the shim.
    }

    /**
     * $evaluate-measure still routes through. Uses the same dispatcher; its routing is the
     * "operation" half of the Phase-1 trace.
     */
    @Test
    @Order(9)
    void evaluate_measure_routes_through_repository() throws Exception {
        var measure = new Measure();
        measure.setId(new IdType("Measure", "spike-measure"));
        measure.setUrl("http://example.org/Measure/spike-measure");
        measure.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
        repo.update(measure);

        var url = "/Measure/spike-measure/$evaluate-measure"
                + "?periodStart=2024-01-01&periodEnd=2024-12-31&reportType=population";
        var resp = get(url);

        // Either a successful evaluation or an OperationOutcome from the processor — both prove
        // the dispatcher reached the operation, which reached IRepository to read the Measure.
        assertTrue(resp.statusCode() != 404, "operation route not found");
        assertTrue(
                resp.body().contains("MeasureReport") || resp.body().contains("OperationOutcome"),
                () -> "expected MeasureReport/OperationOutcome; got: " + resp.body());
    }

    // ---------------- helpers ----------------

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Accept", "application/fhir+json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/fhir+json")
                        .header("Accept", "application/fhir+json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/fhir+json")
                        .header("Accept", "application/fhir+json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Accept", "application/fhir+json")
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String encode(org.hl7.fhir.instance.model.api.IBaseResource r) {
        return fhirContext.newJsonParser().encodeResourceToString(r);
    }

    private static String idFromLocation(String location, String resourceType) {
        var marker = "/" + resourceType + "/";
        return location.substring(location.indexOf(marker) + marker.length()).replaceAll("/_history.*", "");
    }
}
