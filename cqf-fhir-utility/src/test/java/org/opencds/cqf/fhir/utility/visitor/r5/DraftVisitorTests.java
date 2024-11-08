package org.opencds.cqf.fhir.utility.visitor.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r5.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r5.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory;
import org.opencds.cqf.fhir.utility.r5.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.visitor.DraftVisitor;
import org.opencds.cqf.fhir.utility.visitor.IKnowledgeArtifactVisitor;

class DraftVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private Repository repo;
    private final IParser jsonParser = fhirContext.newJsonParser();
    private final String specificationLibReference = "Library/SpecificationLibrary";
    private final List<String> badVersionList = Arrays.asList(
            "11asd1",
            "1.1.3.1.1",
            "1.|1.1.1",
            "1/.1.1.1",
            "-1.-1.2.1",
            "1.-1.2.1",
            "1.1.-2.1",
            "7.1..21",
            "1.2.1.3-draft",
            "1.2.3-draft",
            "3.2",
            "1.",
            "3.ad.2.",
            "1.0.0.1",
            "",
            null);

    @BeforeEach
    void setup() {
        repo = new InMemoryFhirRepository(fhirContext);
    }

    @Test
    void library_draft_test() {
        Bundle bundle = (Bundle)
                jsonParser.parseResource(DraftVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor();
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
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(draftVisitor, repo, params);
        assertNotNull(returnedBundle);
        Optional<BundleEntryComponent> maybeLib = returnedBundle.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findAny();
        assertTrue(maybeLib.isPresent());
        Library lib =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        assertNotNull(lib);
        assertTrue(lib.getStatus() == Enumerations.PublicationStatus.DRAFT);
        assertEquals(lib.getVersion(), draftedVersion);
        assertFalse(lib.hasApprovalDate());
        assertFalse(lib.hasExtension(IKnowledgeArtifactAdapter.RELEASE_DESCRIPTION_URL));
        assertFalse(lib.hasExtension(IKnowledgeArtifactAdapter.RELEASE_LABEL_URL));
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
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor();
        PlanDefinition planDef = repo.read(PlanDefinition.class, new IdType("PlanDefinition/us-ecr-specification"))
                .copy();
        assertTrue(planDef.hasEffectivePeriod());
        String version = "1.01.21";
        Parameters params = parameters(part("version", version));
        Bundle returnedBundle = (Bundle) libraryAdapter.accept(draftVisitor, repo, params);

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
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor();

        try {
            libraryAdapter.accept(draftVisitor, repo, params);

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
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor();
        try {
            libraryAdapter.accept(draftVisitor, repo, params);
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
        IKnowledgeArtifactVisitor draftVisitor = new DraftVisitor();

        for (String version : badVersionList) {
            UnprocessableEntityException maybeException = null;
            Parameters params = parameters(part("version", new StringType(version)));
            try {
                libraryAdapter.accept(draftVisitor, repo, params);
            } catch (UnprocessableEntityException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
    }
}
