package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ClasspathUtil;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class HapiArtifactDiffProcessorTest {

    public HapiArtifactDiffProcessor artifactDiffProcessor;

    @Test
    void testArtifactDiffProcessor_compareComputable() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        artifactDiffProcessor = new HapiArtifactDiffProcessor(repository);

        Bundle sourceBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-active-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-drafted-bundle.json");
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
        boolean compareComputable = true;
        boolean compareExecutable = false;

        Parameters diff = (Parameters)
                artifactDiffProcessor.getArtifactDiff(source, target, compareComputable, compareExecutable, null, null);
        Assertions.assertNotNull(diff);
        List<Parameters> nestedChanges = diff.getParameter().stream()
                .filter(p -> !p.getName().equals("operation"))
                .map(p -> (Parameters) p.getResource())
                .filter(Objects::nonNull)
                .filter(p -> !p.getParameter().isEmpty())
                .toList();
        assertEquals(6, nestedChanges.size());
        Parameters grouperChanges = diff.getParameter().stream()
                .filter(p -> p.getName().contains("/dxtc"))
                .map(p -> (Parameters) p.getResource())
                .findFirst()
                .get();
        List<Parameters.ParametersParameterComponent> deleteOperations =
                getOperationsByType(grouperChanges.getParameter(), "delete");
        List<Parameters.ParametersParameterComponent> insertOperations =
                getOperationsByType(grouperChanges.getParameter(), "insert");
        // delete 2 leafs and extensions
        assertEquals(5, deleteOperations.size());
        // there aren't actually 2 operations here
        assertEquals(2, insertOperations.size());
        String path1 = insertOperations.get(0).getPart().stream()
                .filter(p -> p.getName().equals("path"))
                .map(p -> ((StringType) p.getValue()).getValue())
                .findFirst()
                .get();
        String path2 = insertOperations.get(1).getPart().stream()
                .filter(p -> p.getName().equals("path"))
                .map(p -> ((StringType) p.getValue()).getValue())
                .findFirst()
                .get();
        // insert the new leaf; adding a node takes multiple operations if
        // the thing being added isn't a defined complex FHIR type
        assertEquals("ValueSet.compose.include", path1);
        assertEquals("ValueSet.compose.include[1].valueSet", path2);
    }

    @Test
    void testArtifactDiffProcessor_compareExecutable() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        artifactDiffProcessor = new HapiArtifactDiffProcessor(repository);

        Bundle sourceBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-active-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-drafted-bundle.json");
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
        boolean compareComputable = false;
        boolean compareExecutable = true;

        Parameters diff = (Parameters)
                artifactDiffProcessor.getArtifactDiff(source, target, compareComputable, compareExecutable, null, null);
        Assertions.assertNotNull(diff);
        List<Parameters> nestedChanges = diff.getParameter().stream()
                .filter(p -> !p.getName().equals("operation"))
                .map(p -> (Parameters) p.getResource())
                .filter(Objects::nonNull)
                .filter(p -> !p.getParameter().isEmpty())
                .toList();
        assertEquals(6, nestedChanges.size());
        Parameters grouperChanges = diff.getParameter().stream()
                .filter(p -> p.getName().contains("/dxtc"))
                .map(p -> (Parameters) p.getResource())
                .findFirst()
                .get();
        List<Parameters.ParametersParameterComponent> deleteOperations =
                getOperationsByType(grouperChanges.getParameter(), "delete");
        List<Parameters.ParametersParameterComponent> insertOperations =
                getOperationsByType(grouperChanges.getParameter(), "insert");

        // old codes removed
        assertEquals(32, deleteOperations.size());
        // new codes added
        assertEquals(40, insertOperations.size());
    }

    private List<Parameters.ParametersParameterComponent> getOperationsByType(
            List<Parameters.ParametersParameterComponent> parameters, String type) {
        return parameters.stream()
                .filter(p -> p.getName().equals("operation")
                        && p.getPart().stream()
                                .anyMatch(part -> part.getName().equals("type")
                                        && ((CodeType) part.getValue())
                                                .getCode()
                                                .equals(type)))
                .collect(Collectors.toList());
    }

    /**
     * Regression: complex extensions (e.g. the {@code package-source} extension emitted by
     * {@code $data-requirements}) have no top-level value — only nested sub-extensions. The
     * earlier {@code extensionEquals} implementation dereferenced {@code extension.getValue()}
     * directly and NPE'd on these, blowing up {@code $create-changelog}. The fix falls back to
     * {@link Extension#equalsDeep} when either side has no top-level value.
     */
    @Test
    void extensionEquals_complexExtensions_withNoTopLevelValue() throws Exception {
        var method =
                HapiArtifactDiffProcessor.class.getDeclaredMethod("extensionEquals", Extension.class, Extension.class);
        method.setAccessible(true);

        var sourceComplex = buildPackageSourceExtension("hl7.fhir.us.core", "6.1.0");
        var targetComplex = buildPackageSourceExtension("hl7.fhir.us.core", "6.1.0");

        assertTrue(
                (boolean) method.invoke(null, sourceComplex, targetComplex),
                "two structurally identical complex extensions should be equal");

        var differingComplex = buildPackageSourceExtension("hl7.fhir.us.core", "7.0.0");
        assertFalse(
                (boolean) method.invoke(null, sourceComplex, differingComplex),
                "complex extensions differing in a nested value should not be equal");

        var simpleExtension =
                new Extension("http://example.org/StructureDefinition/simple", new StringType("anything"));
        assertFalse(
                (boolean) method.invoke(null, sourceComplex, simpleExtension),
                "complex extension and value-bearing extension should not be equal");
        assertFalse(
                (boolean) method.invoke(null, simpleExtension, sourceComplex),
                "value-bearing extension and complex extension should not be equal");
    }

    /**
     * Regression: HAPI's {@code Parameters.getParameter(String)} NPEs when iterating a list that
     * contains a component with a null name. That state arises in this processor whenever a
     * malformed relatedArtifact resource URL parses to {@code null} via {@code Canonicals.getUrl}
     * and is stored back on a component, breaking subsequent lookups by name. The null-safe
     * {@code hasParameterNamed} helper guards both null names in the list and a null search key.
     */
    @Test
    void hasParameterNamed_isNullSafe() throws Exception {
        var method =
                HapiArtifactDiffProcessor.class.getDeclaredMethod("hasParameterNamed", Parameters.class, String.class);
        method.setAccessible(true);

        var params = new Parameters();
        params.addParameter().setName("http://example.org/foo"); // named
        params.addParameter().setValue(new StringType("orphan")); // no name set

        assertTrue((boolean) method.invoke(null, params, "http://example.org/foo"));
        assertFalse((boolean) method.invoke(null, params, "http://example.org/bar"));
        assertFalse((boolean) method.invoke(null, params, (String) null), "null search key should never match");
        assertFalse(
                (boolean) method.invoke(null, (Parameters) null, "anything"),
                "null parameters collection should not throw");
    }

    private static Extension buildPackageSourceExtension(String packageId, String version) {
        var ext = new Extension("http://hl7.org/fhir/StructureDefinition/package-source");
        ext.addExtension("packageId", new StringType(packageId));
        ext.addExtension("version", new StringType(version));
        return ext;
    }

    /**
     * Regression: contained expansion parameters (Library.contained[0].parameter[]) are an
     * order-independent set, but FhirPatch.diff compares arrays positionally. A manifest whose
     * expansion params are merely reordered previously produced a cascade of meaningless replace
     * operations pairing unrelated entries by index. With set-based matching + reordering, a pure
     * reorder yields no parameter-path operations.
     */
    @Test
    void artifactDiff_reorderedExpansionParams_produceNoOps() {
        var processor = new HapiArtifactDiffProcessor(new InMemoryFhirRepository(FhirContext.forR4()));
        var source = manifestWithExpansionParams("1.0.0", "http://loinc.org|2.81", "http://snomed.info/sct|2024");
        var target = manifestWithExpansionParams("1.0.0", "http://snomed.info/sct|2024", "http://loinc.org|2.81");

        var diff = (Parameters) processor.getArtifactDiff(source, target, true, true, null, null);

        assertTrue(
                operationsWithPathContaining(diff, "parameter").isEmpty(),
                "reordered-but-unchanged expansion params should produce no operations");
    }

    @Test
    void artifactDiff_versionBumpOnSameSystem_producesSingleReplace() {
        var processor = new HapiArtifactDiffProcessor(new InMemoryFhirRepository(FhirContext.forR4()));
        var source = manifestWithExpansionParams("1.0.0", "http://loinc.org|2.81");
        var target = manifestWithExpansionParams("1.0.0", "http://loinc.org|2.82");

        var diff = (Parameters) processor.getArtifactDiff(source, target, true, true, null, null);

        var paramOps = operationsWithPathContaining(diff, "parameter");
        assertEquals(1, paramOps.size(), "a same-system version bump should be a single replace");
        assertEquals(
                "replace",
                ((CodeType) paramOps.get(0).getPart().stream()
                                .filter(p -> "type".equals(p.getName()))
                                .findFirst()
                                .orElseThrow()
                                .getValue())
                        .getCode());
    }

    @Test
    void artifactDiff_addedExpansionParam_producesInsert() {
        var processor = new HapiArtifactDiffProcessor(new InMemoryFhirRepository(FhirContext.forR4()));
        var source = manifestWithExpansionParams("1.0.0", "http://loinc.org|2.81");
        var target = manifestWithExpansionParams("1.0.0", "http://loinc.org|2.81", "http://snomed.info/sct|2024");

        var diff = (Parameters) processor.getArtifactDiff(source, target, true, true, null, null);

        var paramOps = operationsWithPathContaining(diff, "parameter");
        assertFalse(paramOps.isEmpty(), "an added expansion param should produce at least one operation");
        assertTrue(
                paramOps.stream()
                        .anyMatch(op -> op.getPart().stream()
                                .anyMatch(p -> "type".equals(p.getName())
                                        && p.getValue() instanceof CodeType ct
                                        && "insert".equals(ct.getCode()))),
                "the added param should surface as an insert");
    }

    /**
     * An unchanged dependency (identical version-pinned canonical on both sides) is the same
     * immutable artifact — the recursion short-circuits, so there is no sub-diff, no relatedArtifact
     * operation, and (since the placeholder was removed) no retrieval-failure entry.
     */
    @Test
    void artifactDiff_unchangedDependency_producesNoEntries() {
        var processor = new HapiArtifactDiffProcessor(new InMemoryFhirRepository(FhirContext.forR4()));
        var ig = "http://hl7.org/fhir/us/core/ImplementationGuide/hl7.fhir.us.core|6.1.0";
        var source = manifestWithComposedOf("1.0.0", ig);
        var target = manifestWithComposedOf("1.0.0", ig);

        var diff = (Parameters) processor.getArtifactDiff(source, target, true, true, null, null);

        assertFalse(hasRetrievalFailureEntry(diff), "unchanged dependency should not emit a retrieval-failure entry");
        assertTrue(
                operationsWithPathContaining(diff, "relatedArtifact").isEmpty(),
                "unchanged dependency should produce no relatedArtifact operation");
    }

    /**
     * A dependency whose version changed (e.g. a pin removed by the data-requirements stub fix) is
     * reported once, at the reference level, as a relatedArtifact replace. The recursive sub-diff
     * can't resolve the (external) resource, but that no longer pollutes the payload with a
     * placeholder — the change is carried solely by the relatedArtifact operation.
     */
    @Test
    void artifactDiff_versionChangedDependency_reportedAtReferenceLevelOnly() {
        var processor = new HapiArtifactDiffProcessor(new InMemoryFhirRepository(FhirContext.forR4()));
        var source = manifestWithComposedOf("1.0.0", "http://loinc.org|2.82");
        var target = manifestWithComposedOf("1.0.0", "http://loinc.org");

        var diff = (Parameters) processor.getArtifactDiff(source, target, true, true, null, null);

        assertFalse(
                hasRetrievalFailureEntry(diff),
                "unresolvable external code system should not emit a retrieval-failure entry");
        assertFalse(
                operationsWithPathContaining(diff, "relatedArtifact").isEmpty(),
                "the version change should still be reported as a relatedArtifact operation");
    }

    private static boolean hasRetrievalFailureEntry(Parameters diff) {
        return diff.getParameter().stream()
                .anyMatch(p -> p.getValue() instanceof StringType st
                        && st.getValue() != null
                        && st.getValue().contains("could not be retrieved"));
    }

    private static Library manifestWithComposedOf(String version, String composedOfCanonical) {
        var lib = new Library();
        lib.setId("Library/test-manifest");
        lib.setUrl("http://example.org/Library/test-manifest");
        lib.setVersion(version);
        lib.addRelatedArtifact()
                .setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource(composedOfCanonical);
        return lib;
    }

    private static List<Parameters.ParametersParameterComponent> operationsWithPathContaining(
            Parameters diff, String pathFragment) {
        return diff.getParameter().stream()
                .filter(p -> "operation".equals(p.getName()))
                .filter(op -> op.getPart().stream()
                        .anyMatch(part -> "path".equals(part.getName())
                                && part.getValue() instanceof StringType st
                                && st.getValue() != null
                                && st.getValue().contains(pathFragment)))
                .collect(Collectors.toList());
    }

    private static Library manifestWithExpansionParams(String version, String... systemVersions) {
        var lib = new Library();
        lib.setId("Library/test-manifest");
        lib.setUrl("http://example.org/Library/test-manifest");
        lib.setVersion(version);
        var params = new Parameters();
        params.setId("expansion-parameters");
        for (var sv : systemVersions) {
            params.addParameter("system-version", new org.hl7.fhir.r4.model.UriType(sv));
        }
        lib.addContained(params);
        lib.addExtension(
                "http://hl7.org/fhir/StructureDefinition/cqf-expansionParameters",
                new org.hl7.fhir.r4.model.Reference("#expansion-parameters"));
        return lib;
    }
}
