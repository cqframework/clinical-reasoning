package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ValueSetComposeWalkerTest {
    private FhirContext fhirContext;
    private InMemoryFhirRepository repo;

    @BeforeEach
    void setup() {
        fhirContext = FhirContext.forR4Cached();
        repo = new InMemoryFhirRepository(fhirContext);
    }

    private ValueSet createValueSet(String url, List<String> systems, List<String> valueSetIncludes) {
        var vs = new ValueSet();
        vs.setId(url.substring(url.lastIndexOf("/") + 1));
        vs.setUrl(url);
        var compose = vs.getCompose();
        for (var system : systems) {
            compose.addInclude().setSystem(system);
        }
        for (var vsUrl : valueSetIncludes) {
            compose.addInclude().addValueSet(vsUrl);
        }
        return vs;
    }

    private ConformanceResourceResolver createResolver() {
        return new ConformanceResourceResolver(repo, Collections.emptyList(), Collections.emptyList());
    }

    @Test
    void walkComposeChains_emptyInput_returnsEmpty() {
        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(Set.of());

        assertTrue(result.transitiveCodeSystems().isEmpty());
        assertTrue(result.transitiveValueSets().isEmpty());
    }

    @Test
    void walkComposeChains_valueSetWithCodeSystem_discoversCodeSystem() {
        var vs = createValueSet(
                "http://example.org/ValueSet/test-vs",
                List.of("http://loinc.org", "http://snomed.info/sct"),
                List.of());
        repo.create(vs);

        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(Set.of("http://example.org/ValueSet/test-vs"));

        assertEquals(2, result.transitiveCodeSystems().size());
        assertTrue(result.transitiveCodeSystems().contains("http://loinc.org"));
        assertTrue(result.transitiveCodeSystems().contains("http://snomed.info/sct"));
        assertTrue(result.transitiveValueSets().isEmpty());
    }

    @Test
    void walkComposeChains_valueSetIncludingValueSet_discoversTransitiveValueSet() {
        var vsA = createValueSet(
                "http://example.org/ValueSet/vs-a",
                List.of("http://loinc.org"),
                List.of("http://example.org/ValueSet/vs-b"));
        var vsB = createValueSet("http://example.org/ValueSet/vs-b", List.of("http://snomed.info/sct"), List.of());
        repo.create(vsA);
        repo.create(vsB);

        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(Set.of("http://example.org/ValueSet/vs-a"));

        assertTrue(result.transitiveCodeSystems().contains("http://loinc.org"));
        assertTrue(result.transitiveCodeSystems().contains("http://snomed.info/sct"));
        assertTrue(result.transitiveValueSets().contains("http://example.org/ValueSet/vs-b"));
    }

    @Test
    void walkComposeChains_transitiveChain_discoversDeep() {
        var vsA = createValueSet(
                "http://example.org/ValueSet/vs-a", List.of(), List.of("http://example.org/ValueSet/vs-b"));
        var vsB = createValueSet(
                "http://example.org/ValueSet/vs-b", List.of(), List.of("http://example.org/ValueSet/vs-c"));
        var vsC = createValueSet("http://example.org/ValueSet/vs-c", List.of("http://deep.system/cs"), List.of());
        repo.create(vsA);
        repo.create(vsB);
        repo.create(vsC);

        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(Set.of("http://example.org/ValueSet/vs-a"));

        assertTrue(result.transitiveCodeSystems().contains("http://deep.system/cs"));
        assertTrue(result.transitiveValueSets().contains("http://example.org/ValueSet/vs-b"));
        assertTrue(result.transitiveValueSets().contains("http://example.org/ValueSet/vs-c"));
    }

    @Test
    void walkComposeChains_cycle_handlesGracefully() {
        var vsA = createValueSet(
                "http://example.org/ValueSet/vs-a",
                List.of("http://system-a.org"),
                List.of("http://example.org/ValueSet/vs-b"));
        var vsB = createValueSet(
                "http://example.org/ValueSet/vs-b",
                List.of("http://system-b.org"),
                List.of("http://example.org/ValueSet/vs-a"));
        repo.create(vsA);
        repo.create(vsB);

        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(Set.of("http://example.org/ValueSet/vs-a"));

        // Should complete without infinite loop
        assertTrue(result.transitiveCodeSystems().contains("http://system-a.org"));
        assertTrue(result.transitiveCodeSystems().contains("http://system-b.org"));
    }

    @Test
    void walkComposeChains_unresolvedValueSet_continuesGracefully() {
        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(Set.of("http://example.org/ValueSet/nonexistent"));

        // Should not throw, just return empty
        assertTrue(result.transitiveCodeSystems().isEmpty());
        assertTrue(result.transitiveValueSets().isEmpty());
    }

    @Test
    void walkComposeChains_multipleInputs_discoversAll() {
        var vs1 = createValueSet("http://example.org/ValueSet/vs-1", List.of("http://system-1.org"), List.of());
        var vs2 = createValueSet("http://example.org/ValueSet/vs-2", List.of("http://system-2.org"), List.of());
        repo.create(vs1);
        repo.create(vs2);

        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(
                Set.of("http://example.org/ValueSet/vs-1", "http://example.org/ValueSet/vs-2"));

        assertEquals(2, result.transitiveCodeSystems().size());
        assertTrue(result.transitiveCodeSystems().contains("http://system-1.org"));
        assertTrue(result.transitiveCodeSystems().contains("http://system-2.org"));
    }

    @Test
    void walkComposeChains_versionedUrl_stripsVersion() {
        var vs = createValueSet("http://example.org/ValueSet/vs-versioned", List.of("http://loinc.org"), List.of());
        repo.create(vs);

        var walker = new ValueSetComposeWalker(createResolver(), FhirVersionEnum.R4);
        var result = walker.walkComposeChains(Set.of("http://example.org/ValueSet/vs-versioned|1.0.0"));

        assertTrue(result.transitiveCodeSystems().contains("http://loinc.org"));
    }
}
