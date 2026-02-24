package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ClasspathUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.springframework.data.util.StreamUtils;

class HapiCreateChangelogProcessorTest {

    public HapiCreateChangelogProcessor createChangelogProcessor;

    /*    private Parameters createChangelogSetup() {
        loadTransaction("small-diff-bundle.json");
        var bundle = (Bundle) loadTransaction("small-dxtc-modified-diff-bundle.json");
        var maybeLib = bundle.getEntry().stream().filter(entry -> entry.getResponse().getLocation().contains("Library")).findFirst();
        Parameters diffParams = new Parameters();
        diffParams.addParameter("source", specificationLibReference);
        diffParams.addParameter("target", maybeLib.get().getResponse().getLocation());
        var endpoint = new Endpoint();
        endpoint.setAddress("https://cts.nlm.nih.gov/fhir");
        endpoint.addExtension("vsacUsername", new StringType("tahaattarismile"));
        endpoint.addExtension("apiKey", new StringType("e071d986-0c68-4d06-95ee-00602a2bb748"));
        diffParams.addParameter("target", maybeLib.get().getResponse().getLocation());
        // diffParams.addParameter().setName("terminologyEndpoint").setResource( endpoint);
        return diffParams;
    }*/

    @Test
    void create_changelog_pages() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        createChangelogProcessor = new HapiCreateChangelogProcessor(repository);

        Bundle sourceBundle = ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-diff-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-dxtc-modified-diff-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();

        // check that the correct pages are created
        var returnedBinary = (Binary) createChangelogProcessor.createChangelog(source, target, null);
        assertNotNull(returnedBinary);
        byte[] decodedBytes = Base64.getDecoder().decode(returnedBinary.getContentAsBase64());
        String decodedString = new String(decodedBytes);
        ObjectMapper mapper = new ObjectMapper();
        var pageURLS = List.of(
                "http://ersd.aimsplatform.org/fhir/Library/SpecificationLibrary",
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification",
                "http://ersd.aimsplatform.org/fhir/Library/rctc",
                "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc",
                "http://snomed.info/sct");
        assertDoesNotThrow(() -> {
            var node = mapper.readTree(decodedString);
            assertTrue(node.get("pages").isArray());
            var pages = node.get("pages");
            assertEquals(pageURLS.size(), pages.size());
            for (final var url : pageURLS) {
                var pageExists = StreamSupport.stream(pages.spliterator(), false)
                        .anyMatch(page -> page.get("url").asText().equals(url));
                assertTrue(pageExists);
            }
        });
    }

    @Test
    void create_changelog_codes() {
        // check that the correct leaf VS codes are generated and have
        // the correct memberOID values
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        createChangelogProcessor = new HapiCreateChangelogProcessor(repository);

        Bundle sourceBundle = ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-diff-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-dxtc-modified-diff-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();

        // check that the correct pages are created
        var returnedBinary = (Binary) createChangelogProcessor.createChangelog(source, target, null);
        assertNotNull(returnedBinary);
        byte[] decodedBytes = Base64.getDecoder().decode(returnedBinary.getContentAsBase64());
        String decodedString = new String(decodedBytes);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, CodeAndOperation> oldCodes = new HashMap<>();
        oldCodes.put("772155008", new CodeAndOperation("2.16.840.1.113883.3.464.1003.113.11.1090", null));
        oldCodes.put("1086051000119107", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("1086061000119109", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("1086071000119103", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("1090211000119102", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("129667001", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("13596001", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("15682004", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("186347006", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("18901009", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("194945009", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("230596007", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("240422004", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("26117009", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("276197005", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("276197005", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("3419005", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("397428000", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("397430003", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("48278001", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("50215002", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("715659006", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("75589004", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("7773002", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("789005009", new CodeAndOperation("2.16.840.1.113762.1.4.1146.6", "delete"));
        oldCodes.put("127631000119105", new CodeAndOperation("fake.oid.to.trigger.naive.expansion", null));
        oldCodes.put("15693281000119105", new CodeAndOperation("fake.oid.to.trigger.naive.expansion", "delete"));
        var newCodes = new HashMap<String, CodeAndOperation>();
        newCodes.put("772155008", new CodeAndOperation("2.16.840.1.113883.3.464.1003.113.11.1090", null));
        newCodes.put("1193749009", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("1193750009", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("240349003", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("240350003", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("240351004", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("447282003", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("63650001", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("81020007", new CodeAndOperation("2.16.840.1.113762.1.4.1146.163", "insert"));
        newCodes.put("127631000119105", new CodeAndOperation("fake.oid.to.trigger.naive.expansion", null));
        newCodes.put("15693201000119102", new CodeAndOperation("fake.oid.to.trigger.naive.expansion", "insert"));
        newCodes.put("15693241000119100", new CodeAndOperation("fake.oid.to.trigger.naive.expansion", "insert"));

        assertDoesNotThrow(() -> {
            var node = mapper.readTree(decodedString);
            assertTrue(node.get("pages").isArray());
            var pages = node.get("pages");
            for (final var page : pages) {
                if (Canonicals.getResourceType(page.get("url").asText()).equals("ValueSet")) {
                    assertTrue(page.get("oldData").get("codes").isArray());
                    for (final var code : page.get("oldData").get("codes")) {
                        CodeAndOperation expectedOldCode =
                                oldCodes.get(code.get("code").asText());
                        assertNotNull(expectedOldCode);
                        if (expectedOldCode.operation != null) {
                            assertEquals(
                                    expectedOldCode.operation,
                                    code.get("operation").get("type").asText());
                            assertEquals(
                                    expectedOldCode.code, code.get("memberOid").asText());
                        }
                    }
                    assertTrue(page.get("newData").get("codes").isArray());
                    for (final var code : page.get("newData").get("codes")) {
                        CodeAndOperation expectedNewCode =
                                newCodes.get(code.get("code").asText());
                        assertNotNull(expectedNewCode);
                        if (expectedNewCode.operation != null) {
                            assertEquals(
                                    expectedNewCode.operation,
                                    code.get("operation").get("type").asText());
                            assertEquals(
                                    expectedNewCode.code, code.get("memberOid").asText());
                        }
                    }
                }
            }
        });
    }

    @Test
    void create_changelog_conditions_and_priorities() {
        // check that the conditions and priorities are correctly
        // extracted and have the correct operations
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        createChangelogProcessor = new HapiCreateChangelogProcessor(repository);

        Bundle sourceBundle = ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-diff-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-dxtc-modified-diff-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();

        var returnedBinary = (Binary) createChangelogProcessor.createChangelog(source, target, null);
        assertNotNull(returnedBinary);
        Map<String, Map<String, List<CodeAndOperation>>> oldLeafsAndConditions = Map.of(
                "2.16.840.1.113883.3.464.1003.113.11.1090",
                        Map.of(
                                "conditions",
                                        List.of(
                                                new CodeAndOperation("49649001", null),
                                                new CodeAndOperation("000000000", "delete")),
                                "priority", List.of(new CodeAndOperation("routine", null))),
                "2.16.840.1.113762.1.4.1146.6",
                        Map.of(
                                "conditions",
                                        List.of(
                                                new CodeAndOperation("49649001", null),
                                                new CodeAndOperation("767146004", null)),
                                "priority", List.of(new CodeAndOperation("emergent", null))),
                "2.16.840.1.113762.1.4.1146.1505",
                        Map.of(
                                "conditions", List.of(new CodeAndOperation("49649001", null)),
                                "priority", List.of(new CodeAndOperation("routine", null))),
                "fake.oid.to.trigger.naive.expansion",
                        Map.of(
                                "conditions", List.of(new CodeAndOperation("49649001", null)),
                                "priority", List.of(new CodeAndOperation("routine", null))));
        Map<String, Map<String, List<CodeAndOperation>>> newLeafsAndConditions = Map.of(
                "2.16.840.1.113883.3.464.1003.113.11.1090",
                        Map.of(
                                "conditions",
                                        List.of(
                                                new CodeAndOperation("767146004", "insert"),
                                                new CodeAndOperation("49649001", null)),
                                "priority", List.of(new CodeAndOperation("emergent", "replace"))),
                "2.16.840.1.113762.1.4.1146.163",
                        Map.of(
                                "conditions", List.of(new CodeAndOperation("123123123", null)),
                                "priority", List.of(new CodeAndOperation("emergent", null))),
                "2.16.840.1.113762.1.4.1146.1505",
                        Map.of(
                                "conditions", List.of(new CodeAndOperation("49649001", null)),
                                "priority", List.of(new CodeAndOperation("routine", null))),
                "fake.oid.to.trigger.naive.expansion",
                        Map.of(
                                "conditions", List.of(new CodeAndOperation("49649001", null)),
                                "priority", List.of(new CodeAndOperation("routine", null))));
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> {
            var node = mapper.readTree(new String(Base64.getDecoder().decode(returnedBinary.getContentAsBase64())));
            assertTrue(node.get("pages").isArray());
            var pages = node.get("pages");
            for (final var page : pages) {
                if (Canonicals.getResourceType(page.get("url").asText()).equals("ValueSet")) {
                    assertTrue(page.get("oldData").get("leafValuesets").isArray());
                    assertTrue(page.get("oldData")
                            .get("priority")
                            .get("value")
                            .asText()
                            .equals("routine"));
                    for (final var leaf : page.get("oldData").get("leafValuesets")) {
                        assertTrue(leaf.get("conditions").isArray());
                        var memberOid = leaf.get("memberOid").asText();
                        assertTrue(oldLeafsAndConditions.containsKey(memberOid));
                        List<CodeAndOperation> expectedConditions =
                                oldLeafsAndConditions.get(memberOid).get("conditions");
                        assertTrue(expectedConditions.size() > 0);
                        for (final var condition : leaf.get("conditions")) {
                            Optional<CodeAndOperation> conditionInList = expectedConditions.stream()
                                    .filter(c -> c.code != null
                                            && c.code.equals(
                                                    condition.get("code").asText()))
                                    .findAny();
                            assertTrue(conditionInList.isPresent());
                            if (conditionInList.get().operation != null) {
                                assertEquals(
                                        conditionInList.get().operation,
                                        condition.get("operation").get("type").asText());
                            }
                        }
                        assertNotNull(leaf.get("priority").get("value"));
                        CodeAndOperation expectedPriority = oldLeafsAndConditions
                                .get(memberOid)
                                .get("priority")
                                .get(0);
                        assertEquals(
                                expectedPriority.code,
                                leaf.get("priority").get("value").asText());
                        if (expectedPriority.operation != null) {
                            assertEquals(
                                    expectedPriority.operation,
                                    leaf.get("priority")
                                            .get("operation")
                                            .get("type")
                                            .asText());
                        }
                    }
                    assertTrue(page.get("newData").get("leafValuesets").isArray());
                    assertTrue(page.get("newData")
                            .get("priority")
                            .get("value")
                            .asText()
                            .equals("routine"));
                    for (final var leaf : page.get("newData").get("leafValuesets")) {
                        assertTrue(leaf.get("conditions").isArray());
                        var memberOid = leaf.get("memberOid").asText();
                        assertTrue(newLeafsAndConditions.containsKey(memberOid));
                        List<CodeAndOperation> expectedConditions =
                                newLeafsAndConditions.get(memberOid).get("conditions");
                        assertTrue(expectedConditions.size() > 0);
                        for (final var condition : leaf.get("conditions")) {
                            Optional<CodeAndOperation> conditionInList = expectedConditions.stream()
                                    .filter(c -> c.code != null
                                            && c.code.equals(
                                                    condition.get("code").asText()))
                                    .findAny();
                            assertTrue(conditionInList.isPresent());
                            if (conditionInList.get().operation != null) {
                                assertEquals(
                                        conditionInList.get().operation,
                                        condition.get("operation").get("type").asText());
                            }
                        }
                        assertNotNull(leaf.get("priority").get("value"));
                        CodeAndOperation expectedPriority = newLeafsAndConditions
                                .get(memberOid)
                                .get("priority")
                                .get(0);
                        assertEquals(
                                expectedPriority.code,
                                leaf.get("priority").get("value").asText());
                        if (expectedPriority.operation != null) {
                            assertEquals(
                                    expectedPriority.operation,
                                    leaf.get("priority")
                                            .get("operation")
                                            .get("type")
                                            .asText());
                        }
                    }
                }
            }
        });
    }

    @Test
    void create_changelog_grouped_leaf() {
        // check that all the grouped leaf valuesets exist
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        createChangelogProcessor = new HapiCreateChangelogProcessor(repository);

        Bundle sourceBundle = ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-diff-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-dxtc-modified-diff-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();

        var returnedBinary = (Binary) createChangelogProcessor.createChangelog(source, target, null);
        assertNotNull(returnedBinary);
        ObjectMapper mapper = new ObjectMapper();
        Exception expectNoException = null;
        var oldLeafs = Map.of(
                "2.16.840.1.113883.3.464.1003.113.11.1090", "",
                "2.16.840.1.113762.1.4.1146.6", "delete",
                "2.16.840.1.113762.1.4.1146.1505", "",
                "fake.oid.to.trigger.naive.expansion", "");
        var newLeafs = Map.of(
                "2.16.840.1.113883.3.464.1003.113.11.1090", "",
                "2.16.840.1.113762.1.4.1146.163", "insert",
                "2.16.840.1.113762.1.4.1146.1505", "",
                "fake.oid.to.trigger.naive.expansion", "");
        assertDoesNotThrow(() -> {
            var node = mapper.readTree(new String(Base64.getDecoder().decode(returnedBinary.getContentAsBase64())));
            assertTrue(node.get("pages").isArray());
            var pages = node.get("pages");
            for (final var page : pages) {
                if (Canonicals.getResourceType(page.get("url").asText()).equals("ValueSet")) {
                    assertTrue(page.get("oldData").get("leafValuesets").isArray());
                    for (final var leaf : page.get("oldData").get("leafValuesets")) {
                        var expectedLeaf = oldLeafs.get(leaf.get("memberOid").asText());
                        assertNotNull(expectedLeaf);
                        if (!expectedLeaf.isBlank()) {
                            assertEquals(
                                    expectedLeaf,
                                    leaf.get("operation").get("type").asText());
                        }
                    }
                    assertTrue(page.get("newData").get("leafValuesets").isArray());
                    for (final var leaf : page.get("newData").get("leafValuesets")) {
                        var expectedLeaf = newLeafs.get(leaf.get("memberOid").asText());
                        assertNotNull(expectedLeaf);
                        if (!expectedLeaf.isBlank()) {
                            assertEquals(
                                    expectedLeaf,
                                    leaf.get("operation").get("type").asText());
                        }
                    }
                }
            }
        });
    }

    @Test
    void create_changelog_extracts_vs_name_and_url() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        createChangelogProcessor = new HapiCreateChangelogProcessor(repository);

        Bundle sourceBundle = ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-diff-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-dxtc-modified-diff-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();

        var returnedBinary = (Binary) createChangelogProcessor.createChangelog(source, target, null);
        assertNotNull(returnedBinary);
        ObjectMapper mapper = new ObjectMapper();
        var oldLeafValueSetNames = List.of(
                "Diagnosis_ProblemTriggersforPublicHealthReporting",
                "DiphtheriaDisordersSNOMED",
                "AnkylosingSpondylitis",
                "AcanthamoebaDiseaseKeratitisDisordersSNOMED");
        var newLeafValueSetNames = List.of(
                "Diagnosis_ProblemTriggersforPublicHealthReporting",
                "AnkylosingSpondylitis",
                "Cholera (Disorders) (SNOMED)",
                "UpdatedName");
        assertDoesNotThrow(() -> {
            var node = mapper.readTree(new String(Base64.getDecoder().decode(returnedBinary.getContentAsBase64())));
            assertTrue(node.get("pages").isArray());
            var pages = node.get("pages");
            for (final var page : pages) {
                if (Canonicals.getResourceType(page.get("url").asText()).equals("ValueSet")) {
                    assertTrue(oldLeafValueSetNames.contains(
                            page.get("oldData").get("name").get("value").asText()));
                    assertTrue(newLeafValueSetNames.contains(
                            page.get("newData").get("name").get("value").asText()));
                }
                if (Canonicals.getIdPart(page.get("url").asText()).equals("dxtc")) {
                    assertTrue(page.get("oldData").get("leafValuesets").isArray());
                    assertEquals(3, page.get("oldData").get("leafValuesets").size());
                    for (final var leaf : page.get("oldData").get("leafValuesets")) {
                        var name = leaf.get("name").asText();
                        assertTrue(oldLeafValueSetNames.contains(name));
                        assertNotNull(leaf.get("codeSystems")
                                .iterator()
                                .next()
                                .get("name")
                                .asText());
                        assertNotNull(leaf.get("codeSystems")
                                .iterator()
                                .next()
                                .get("oid")
                                .asText());
                    }
                    assertTrue(page.get("newData").get("leafValuesets").isArray());
                    assertEquals(3, page.get("newData").get("leafValuesets").size());
                    for (final var leaf : page.get("newData").get("leafValuesets")) {
                        var name = leaf.get("name").asText();
                        assertTrue(newLeafValueSetNames.contains(name));
                        if (leaf.get("url")
                                .asText()
                                .equals(
                                        "https://cts.nlm.nih.gov/fhir/ValueSet/fake.oid.to.trigger.naive.expansion&version")) {
                            assertTrue(leaf.get("name")
                                    .get("operation")
                                    .get("path")
                                    .asText()
                                    .equals("name"));
                            assertTrue(leaf.get("name")
                                    .get("operation")
                                    .get("type")
                                    .asText()
                                    .equals("replace"));
                        }
                    }
                }
            }
        });
    }

    @Test
    void created_deleted_groupers_should_be_visible() throws Exception {
        // check that all the grouped leaf valuesets exist
        // check that all the expansion contains and compose include get operations
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        createChangelogProcessor = new HapiCreateChangelogProcessor(repository);

        Bundle sourceBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-vsm-gen-grouper-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "small-dxtc-modified-diff-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();

        var metadataProperties = List.of("id", "name", "url", "version", "title");
        var versions = List.of(
                "Provisional_2022-01-10",
                "http://snomed.info/sct/731000124108/version/20240301",
                "Provisional_2022-04-25");
        var VSMGrouperCodes = List.of(
                "1010333003",
                "1010334009",
                "106001000119101",
                "10692761000119107",
                "1177120001",
                "123123444111",
                "123123444112",
                "123123444113");
        var VSMGrouperLeafVsets = List.of("2.16.840.1.113762.1.4.1251.40", "2.16.840.1.113762.1.4.1248.138");

        ObjectMapper mapper = new ObjectMapper();
        var returnedBinary = (Binary) createChangelogProcessor.createChangelog(source, target, null);

        assertNotNull(returnedBinary);
        var node = mapper.readTree(new String(Base64.getDecoder().decode(returnedBinary.getContentAsBase64())));
        assertTrue(node.get("pages").isArray());
        var pages = node.get("pages");

        // new grouper was deleted
        var deletedGrouperPage = StreamUtils.createStreamFromIterator(pages.iterator())
                .filter((page) -> page.get("url").asText().contains("www.test.com"))
                .findAny();
        assertTrue(deletedGrouperPage.isPresent());

        // all codes and properties in the grouper should be "insert"
        for (final var property : metadataProperties) {
            // all props have a "delete" operation
            assertTrue(deletedGrouperPage
                    .get()
                    .get("oldData")
                    .get(property)
                    .get("operation")
                    .get("type")
                    .asText()
                    .equals("delete"));
        }

        assertEquals(
                VSMGrouperCodes.size(),
                deletedGrouperPage.get().get("oldData").get("codes").size());
        for (final var code : deletedGrouperPage.get().get("oldData").get("codes")) {
            // all codes have a "delete" operation
            assertTrue(code.get("operation").get("type").asText().equals("delete"));
            assertTrue(VSMGrouperCodes.contains(code.get("code").asText()));
            assertNotNull(code.get("version").asText());
            assertTrue(versions.contains(code.get("version").asText()));
        }

        assertEquals(
                VSMGrouperLeafVsets.size(),
                deletedGrouperPage.get().get("oldData").get("leafValuesets").size());
        for (final var leaf : deletedGrouperPage.get().get("oldData").get("leafValuesets")) {
            // all leaf valuesets have a "delete" operation
            assertTrue(leaf.get("operation").get("type").asText().equals("delete"));
            assertTrue(VSMGrouperLeafVsets.contains(leaf.get("memberOid").asText()));
        }

        // reverse source and target
        var returnedBinary2 = (Binary) createChangelogProcessor.createChangelog(source, target, null);
        assertNotNull(returnedBinary2);
        var node2 = mapper.readTree(new String(Base64.getDecoder().decode(returnedBinary2.getContentAsBase64())));
        assertTrue(node2.get("pages").isArray());
        var pages2 = node2.get("pages");

        //  grouper was created
        var createdGrouperPage = StreamUtils.createStreamFromIterator(pages2.iterator())
                .filter((page) -> page.get("url").asText().contains("www.test.com"))
                .findAny();
        assertTrue(createdGrouperPage.isPresent());
        // all codes and properties should show as inserted
        for (final var property : metadataProperties) {
            assertTrue(createdGrouperPage
                    .get()
                    .get("newData")
                    .get(property)
                    .get("operation")
                    .get("type")
                    .asText()
                    .equals("insert"));
        }

        assertEquals(
                VSMGrouperCodes.size(),
                createdGrouperPage.get().get("newData").get("codes").size());
        for (final var code : createdGrouperPage.get().get("newData").get("codes")) {
            assertTrue(code.get("operation").get("type").asText().equals("insert"));
            assertTrue(VSMGrouperCodes.contains(code.get("code").asText()));
            assertNotNull(code.get("version").asText());
            assertTrue(versions.contains(code.get("version").asText()));
        }

        assertEquals(
                VSMGrouperLeafVsets.size(),
                createdGrouperPage.get().get("newData").get("leafValuesets").size());
        for (final var leaf : createdGrouperPage.get().get("newData").get("leafValuesets")) {
            assertTrue(leaf.get("operation").get("type").asText().equals("insert"));
            assertTrue(VSMGrouperLeafVsets.contains(leaf.get("memberOid").asText()));
        }
    }

    private static class CodeAndOperation {
        public String code;
        public String operation;

        CodeAndOperation(String code, String operation) {
            this.code = code;
            this.operation = operation;
        }
    }
}
