package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.MetadataResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * Unit tests for {@link PackageVisitor#gatherPackageCodeSystems} - the safety filter that decides which
 * package-authored CodeSystems are eligible to be supplied to a remote {@code $expand} via {@code tx-resource}.
 */
class PackageVisitorTxResourceTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    private static CodeSystem codeSystem(String url, CodeSystemContentMode content, int concepts) {
        var cs = new CodeSystem();
        cs.setUrl(url);
        cs.setContent(content);
        for (int i = 0; i < concepts; i++) {
            cs.addConcept().setCode("code-" + i);
        }
        return cs;
    }

    private Bundle bundleOf(IBaseResource... resources) {
        var bundle = new Bundle();
        for (var r : resources) {
            bundle.addEntry().setResource((org.hl7.fhir.r4.model.Resource) r);
        }
        return bundle;
    }

    private List<String> urls(List<IBaseResource> resources) {
        return resources.stream().map(r -> ((MetadataResource) r).getUrl()).collect(Collectors.toList());
    }

    @Test
    void gatherPackageCodeSystems_includesUsableCodeSystems_andFiltersUnusableOnes() {
        var repo = new InMemoryFhirRepository(fhirContext);
        // Concept ceiling of 2: the "big" CodeSystem (3 concepts) must be excluded.
        var settings = TerminologyServerClientSettings.getDefault().setMaxTxResourceCodeSystemConcepts(2);
        var visitor = new PackageVisitor(repo, settings, null);

        var usable = codeSystem("http://example.org/CodeSystem/usable", CodeSystemContentMode.COMPLETE, 2);
        var notPresent = codeSystem("http://example.org/CodeSystem/not-present", CodeSystemContentMode.NOTPRESENT, 5);
        var supplement = codeSystem("http://example.org/CodeSystem/supplement", CodeSystemContentMode.SUPPLEMENT, 1);
        var tooBig = codeSystem("http://example.org/CodeSystem/big", CodeSystemContentMode.COMPLETE, 3);
        // a non-CodeSystem resource should be ignored entirely
        var valueSet = new org.hl7.fhir.r4.model.ValueSet();
        valueSet.setUrl("http://example.org/ValueSet/vs");

        var bundle = bundleOf(usable, notPresent, supplement, tooBig, valueSet);

        var gathered = visitor.gatherPackageCodeSystems(bundle);
        var gatheredUrls = urls(gathered);

        assertEquals(1, gathered.size(), "only the usable CodeSystem should be gathered");
        assertTrue(gatheredUrls.contains("http://example.org/CodeSystem/usable"));
        assertFalse(gatheredUrls.contains("http://example.org/CodeSystem/not-present"), "content=not-present excluded");
        assertFalse(gatheredUrls.contains("http://example.org/CodeSystem/supplement"), "content=supplement excluded");
        assertFalse(gatheredUrls.contains("http://example.org/CodeSystem/big"), "over-ceiling CodeSystem excluded");
    }

    @Test
    void gatherPackageCodeSystems_countsNestedConceptsTowardCeiling() {
        var repo = new InMemoryFhirRepository(fhirContext);
        var settings = TerminologyServerClientSettings.getDefault().setMaxTxResourceCodeSystemConcepts(2);
        var visitor = new PackageVisitor(repo, settings, null);

        // 1 top-level concept + 2 nested = 3 total, exceeding the ceiling of 2.
        var nested = new CodeSystem();
        nested.setUrl("http://example.org/CodeSystem/nested");
        nested.setContent(CodeSystemContentMode.COMPLETE);
        var parent = nested.addConcept().setCode("parent");
        parent.addConcept().setCode("child-1");
        parent.addConcept().setCode("child-2");

        var gathered = visitor.gatherPackageCodeSystems(bundleOf(nested));
        assertTrue(gathered.isEmpty(), "nested concepts must be counted toward the ceiling");
    }
}
