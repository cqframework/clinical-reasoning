package org.opencds.cqf.fhir.cr.visitor.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r5.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.visitor.DraftVisitor;
import org.opencds.cqf.fhir.cr.visitor.ReleaseVisitor;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory;
import org.opencds.cqf.fhir.utility.r5.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class DraftVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private IRepository repo;
    private final IParser jsonParser = fhirContext.newJsonParser();
    private final String specificationLibReference = "Library/SpecificationLibrary";
    private final List<String> badVersionList =
            Arrays.asList("1.|1.1.1", "1/.1.1.1", "1.2.1.3-draft", "1.2.3-draft", "", null);
    private final List<String> nonSemverVersionList = Arrays.asList(
            "11asd1",
            "1.1.3.1.1",
            "-1.-1.2.1",
            "1.-1.2.1",
            "1.1.-2.1",
            "7.1..21",
            "3.2",
            "1.",
            "3.ad.2.",
            "1.0.0.1",
            "2025-09");

    @BeforeEach
    void setup() {
        repo = new InMemoryFhirRepository(fhirContext);
    }

    @Test
    void library_draft_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(DraftVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        String version = "1.0.1";
        String draftedVersion = version + "-draft";
        Parameters params = new Parameters();
        params.addParameter("version", version);
        // Root Artifact must have approval date, releaseLabel and releaseDescription for this test
        assertTrue(library.hasApprovalDate());
        assertTrue(library.hasExtension(IKnowledgeArtifactAdapter.RELEASE_DESCRIPTION_URL));
        assertTrue(library.hasExtension(IKnowledgeArtifactAdapter.RELEASE_LABEL_URL));
        assertTrue(library.hasApprovalDate());
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(draftVisitor, params);
        assertNotNull(returnedBundle);
        assertEquals(4, returnedBundle.getEntry().size());
        Optional<BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findAny();
        assertTrue(maybeLib.isPresent());
        Library lib =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        assertNotNull(lib);
        assertSame(Enumerations.PublicationStatus.DRAFT, lib.getStatus());
        assertEquals(draftedVersion, lib.getVersion());
        assertFalse(lib.hasApprovalDate());
        assertFalse(lib.hasExtension(IKnowledgeArtifactAdapter.RELEASE_DESCRIPTION_URL));
        assertFalse(lib.hasExtension(IKnowledgeArtifactAdapter.RELEASE_LABEL_URL));
        Optional<BundleEntryComponent> maybeGrouper = returnedBundle.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("ValueSet"))
                .findAny();
        assertTrue(maybeGrouper.isPresent());
        ValueSet grouper = repo.read(
                ValueSet.class, new IdType(maybeGrouper.get().getResponse().getLocation()));
        assertNotNull(maybeGrouper);
        grouper.getCompose().getInclude().forEach(include -> {
            assertFalse(include.getValueSet().get(0).getValueAsString().contains("|"));
        });
        List<RelatedArtifact> relatedArtifacts = lib.getRelatedArtifact();
        assertFalse(relatedArtifacts.isEmpty());
        MetadataResourceHelper.forEachMetadataResource(
                returnedBundle.getEntry(),
                resource -> {
                    List<RelatedArtifact> relatedArtifacts2 =
                            new org.opencds.cqf.fhir.utility.adapter.r5.KnowledgeArtifactAdapter(resource)
                                    .getRelatedArtifact();
                    if (relatedArtifacts2 != null && relatedArtifacts2.size() > 0) {
                        for (var relatedArtifact : relatedArtifacts2) {
                            if (IKnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(relatedArtifact)) {
                                assertEquals(Canonicals.getVersion(relatedArtifact.getResource()), draftedVersion);
                            }
                        }
                    }
                },
                repo);
    }

    @Test
    void draftOperation_no_effectivePeriod_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(DraftVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        Library baseLib = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        assertTrue(baseLib.hasEffectivePeriod());
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor(repo);
        PlanDefinition planDef = repo.read(PlanDefinition.class, new IdType("PlanDefinition/us-ecr-specification"))
                .copy();
        assertTrue(planDef.hasEffectivePeriod());
        String version = "1.01.21";
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(draftVisitor, params);

        MetadataResourceHelper.forEachMetadataResource(
                returnedBundle.getEntry(),
                resource -> {
                    ILibraryAdapter adapter = new AdapterFactory().createLibrary(baseLib);
                    assertFalse(((Period) adapter.getEffectivePeriod()).hasStart()
                            || ((Period) adapter.getEffectivePeriod()).hasEnd());
                },
                repo);
    }

    @Test
    void draftOperation_version_conflict_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(DraftVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        Library versionConflictLibrary = (Library)
                jsonParser.parseResource(DraftVisitorTests.class.getResourceAsStream("Library-version-conflict.json"));
        repo.transaction(bundle);
        repo.update(versionConflictLibrary);
        Parameters params = parameters(part("version", "1.2.3"));
        String maybeException = null;
        Library baseLib =
                repo.read(Library.class, new IdType(specificationLibReference)).copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor(repo);

        try {
            libraryAdapter.accept(draftVisitor, params);

        } catch (Exception e) {
            maybeException = e.getMessage();
        }
        assertNotNull(maybeException);
        assertTrue(maybeException.contains("already exists"));
    }

    @Test
    void draftOperation_cannot_create_draft_of_draft_test() {
        Library versionConflictLibrary = (Library)
                jsonParser.parseResource(DraftVisitorTests.class.getResourceAsStream("Library-version-conflict.json"));
        repo.update(versionConflictLibrary);
        Parameters params = parameters(part("version", "1.2.1"));
        String maybeException = "";
        Library baseLib = repo.read(Library.class, new IdType("Library/SpecificationLibraryDraftVersion-1-0-0-23"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor(repo);
        try {
            libraryAdapter.accept(draftVisitor, params);
        } catch (PreconditionFailedException e) {
            maybeException = e.getMessage();
        }
        assertNotNull(maybeException);
        assertTrue(maybeException.contains("status of 'active'"));
    }

    @Test
    void draftOperation_version_format_test() {
        Library versionConflictLibrary = (Library)
                jsonParser.parseResource(DraftVisitorTests.class.getResourceAsStream("Library-version-conflict.json"));
        repo.update(versionConflictLibrary);
        Library baseLib = repo.read(Library.class, new IdType("Library/SpecificationLibraryDraftVersion-1-0-0-23"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(baseLib);
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor(repo);

        for (String version : badVersionList) {
            UnprocessableEntityException maybeException = null;
            Parameters params = parameters(part("version", new StringType(version)));
            try {
                libraryAdapter.accept(draftVisitor, params);
            } catch (UnprocessableEntityException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
        // Non-semver versions should warn but not throw UnprocessableEntityException for format
        for (String version : nonSemverVersionList) {
            var lib = repo.read(Library.class, new IdType("Library/SpecificationLibraryDraftVersion-1-0-0-23"))
                    .copy();
            var adapter = new AdapterFactory().createLibrary(lib);
            Parameters params = parameters(part("version", new StringType(version)));
            UnprocessableEntityException versionException = null;
            try {
                adapter.accept(draftVisitor, params);
            } catch (UnprocessableEntityException e) {
                versionException = e;
            } catch (Exception e) {
                // Other exceptions are not version-related
            }
            assertNull(versionException, "Non-semver version '" + version + "' should not throw for format");
        }
    }

    @Test
    void draft_should_restore_authored_expansion_params_after_release() {
        var bundle = (Bundle) jsonParser.parseResource(
                DraftVisitorTests.class.getResourceAsStream("Bundle-versioned-and-unversioned-dependency.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        var originalLibrary = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var releaseLibraryAdapter = new AdapterFactory().createLibrary(originalLibrary.copy());
        var releaseParams =
                parameters(part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType("force")));
        var releaseReturnBundle = (Bundle) releaseLibraryAdapter.accept(releaseVisitor, releaseParams);
        var maybeReleasedLib = releaseReturnBundle.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeReleasedLib.isPresent());
        var releasedLibrary = repo.read(
                Library.class, new IdType(maybeReleasedLib.get().getResponse().getLocation()));

        // Sanity check the release step actually captured the author's input separately from the
        // processing timen exp-params
        var releasedInputExt = releasedLibrary.getExtensionByUrl(Constants.CQF_INPUT_EXPANSION_PARAMETERS);
        assertNotNull(releasedInputExt);
        var releasedInputParams =
                (Parameters) releasedLibrary.getContained(((Reference) releasedInputExt.getValue()).getReference());
        assertEquals(1, releasedInputParams.getParameter().size());
        var releasedExpExt = releasedLibrary.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS);
        assertNotNull(releasedExpExt);
        var releasedExpParams =
                (Parameters) releasedLibrary.getContained(((Reference) releasedExpExt.getValue()).getReference());
        assertEquals(3, releasedExpParams.getParameter().size());

        // Now draft the released library and confirm exp-params is reset to the 1 authored
        // parameter, and input-exp-params (and its extension) are gone
        var draftLibraryAdapter = new AdapterFactory().createLibrary(releasedLibrary.copy());
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor(repo);
        var draftParams = parameters(part("version", new StringType("1.2.4")));
        var draftReturnBundle = (Bundle) draftLibraryAdapter.accept(draftVisitor, draftParams);
        var maybeDraftLib = draftReturnBundle.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .map(entry ->
                        repo.read(Library.class, new IdType(entry.getResponse().getLocation())))
                .filter(lib -> originalLibrary.getUrl().equals(lib.getUrl()))
                .findFirst();
        assertTrue(maybeDraftLib.isPresent());
        var draftLibrary = maybeDraftLib.get();

        assertNull(draftLibrary.getExtensionByUrl(Constants.CQF_INPUT_EXPANSION_PARAMETERS));
        assertNull(draftLibrary.getContained("#input-exp-params"));
        var draftExpExt = draftLibrary.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS);
        assertNotNull(draftExpExt);
        var draftExpParams =
                (Parameters) draftLibrary.getContained(((Reference) draftExpExt.getValue()).getReference());
        assertEquals(1, draftExpParams.getParameter().size());
        assertEquals(
                "http://loinc.org|2.76",
                ((UriType) draftExpParams.getParameter().get(0).getValue()).getValue());
    }

    @Test
    void restoreInputExpansionParams_copies_authored_params_and_cleans_up() {
        var library = new Library();
        library.setId("test-library");

        var inputExpParams = new Parameters();
        inputExpParams.setId("input-exp-params");
        inputExpParams.addParameter("authored-param", new StringType("authored-value"));
        library.addContained(inputExpParams);
        library.addExtension(
                new Extension(Constants.CQF_INPUT_EXPANSION_PARAMETERS, new Reference("#input-exp-params")));

        var expParams = new Parameters();
        expParams.setId("exp-params");
        expParams.addParameter("system-version", new StringType("http://example.com|1.0.0"));
        expParams.addParameter("system-version", new StringType("http://example.com/other|2.0.0"));
        library.addContained(expParams);
        library.addExtension(new Extension(Constants.CQF_EXPANSION_PARAMETERS, new Reference("#exp-params")));

        var adapter = new AdapterFactory().createLibrary(library);
        org.opencds.cqf.fhir.cr.visitor.r5.DraftVisitor.restoreInputExpansionParams(adapter);

        // assert input-exp-params contained element and extension have been removed
        assertNull(library.getExtensionByUrl(Constants.CQF_INPUT_EXPANSION_PARAMETERS));
        assertNull(library.getContained("#input-exp-params"));

        // assert params from input-exp-params have been copied to exp-params
        var restoredExpParamsExt = library.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS);
        assertNotNull(restoredExpParamsExt);
        var restoredExpParams =
                (Parameters) library.getContained(((Reference) restoredExpParamsExt.getValue()).getReference());
        assertEquals(1, restoredExpParams.getParameter().size());
        assertEquals(
                "authored-value",
                ((StringType) restoredExpParams.getParameter().get(0).getValue()).getValue());
    }

    @Test
    void restoreInputExpansionParams_resets_to_empty_when_author_specified_none() {
        var library = new Library();
        library.setId("test-library");

        // input-exp-params is present no params were authored
        var inputExpParams = new Parameters();
        inputExpParams.setId("input-exp-params");
        library.addContained(inputExpParams);
        library.addExtension(
                new Extension(Constants.CQF_INPUT_EXPANSION_PARAMETERS, new Reference("#input-exp-params")));

        // exp-params has processing-time entries which should be overwritten
        var expParams = new Parameters();
        expParams.setId("exp-params");
        expParams.addParameter("system-version", new StringType("http://example.com|1.0.0"));
        library.addContained(expParams);
        library.addExtension(new Extension(Constants.CQF_EXPANSION_PARAMETERS, new Reference("#exp-params")));

        var adapter = new AdapterFactory().createLibrary(library);
        org.opencds.cqf.fhir.cr.visitor.r5.DraftVisitor.restoreInputExpansionParams(adapter);

        // assert input-exp-params contained element and extension have been removed
        assertNull(library.getExtensionByUrl(Constants.CQF_INPUT_EXPANSION_PARAMETERS));
        assertNull(library.getContained("#input-exp-params"));

        // assert empty params from input-exp-params have been copied to exp-params
        var restoredExpParamsExt = library.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS);
        assertNotNull(restoredExpParamsExt);
        var restoredExpParams =
                (Parameters) library.getContained(((Reference) restoredExpParamsExt.getValue()).getReference());
        assertTrue(restoredExpParams.getParameter().isEmpty());
    }

    @Test
    void restoreInputExpansionParams_removes_dangling_extension_when_contained_resource_missing() {
        var library = new Library();
        library.setId("test-library");
        // Extension references a contained resource that was never actually added
        library.addExtension(
                new Extension(Constants.CQF_INPUT_EXPANSION_PARAMETERS, new Reference("#input-exp-params")));

        var adapter = new AdapterFactory().createLibrary(library);
        org.opencds.cqf.fhir.cr.visitor.r5.DraftVisitor.restoreInputExpansionParams(adapter);

        // assert dangling extension was removed
        assertNull(library.getExtensionByUrl(Constants.CQF_INPUT_EXPANSION_PARAMETERS));
        // Nothing to restore from, so exp-params should never have been created
        assertNull(library.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS));
        assertTrue(library.getContained().isEmpty());
    }

    @Test
    void restoreInputExpansionParams_is_noop_when_no_extension_present() {
        var library = new Library();
        library.setId("test-library");

        var adapter = new AdapterFactory().createLibrary(library);
        org.opencds.cqf.fhir.cr.visitor.r5.DraftVisitor.restoreInputExpansionParams(adapter);

        assertTrue(library.getExtension().isEmpty());
        assertTrue(library.getContained().isEmpty());
    }
}
