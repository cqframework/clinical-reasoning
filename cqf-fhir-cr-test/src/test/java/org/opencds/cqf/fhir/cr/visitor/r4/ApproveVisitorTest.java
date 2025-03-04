package org.opencds.cqf.fhir.cr.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.Optional;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.visitor.ApproveVisitor;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ApproveVisitorTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private Repository repo;
    private final IParser jsonParser = fhirContext.newJsonParser();

    @BeforeEach
    void setup() {
        var lib = (Library)
                jsonParser.parseResource(ReleaseVisitorTests.class.getResourceAsStream("Library-ersd-active.json"));
        repo = new InMemoryFhirRepository(fhirContext);
        repo.update(lib);
    }

    @Test
    void approveOperation_endpoint_id_should_match_target_parameter() {
        var artifactAssessmentTarget = "Library/This-Library-Does-Not-Exist|1.0.0";
        var params = parameters(part("artifactAssessmentTarget", new CanonicalType(artifactAssessmentTarget)));
        UnprocessableEntityException maybeException = null;
        var releaseVisitor = new ApproveVisitor(repo);
        var lib = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var libraryAdapter = new AdapterFactory().createLibrary(lib);

        try {
            libraryAdapter.accept(releaseVisitor, params);
        } catch (UnprocessableEntityException e) {
            maybeException = e;
        }
        assertNotNull(maybeException);
        assertTrue(maybeException.getMessage().contains("URL"));
        maybeException = null;
        artifactAssessmentTarget = "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|this-version-is-wrong";
        params = parameters(part("artifactAssessmentTarget", new CanonicalType(artifactAssessmentTarget)));
        try {
            libraryAdapter.accept(releaseVisitor, params);
        } catch (UnprocessableEntityException e) {
            maybeException = e;
        }
        assertNotNull(maybeException);
        assertTrue(maybeException.getMessage().contains("version"));
    }

    @Test
    void approveOperation_should_respect_artifactAssessment_information_type_binding() {
        String artifactAssessmentType = "this-type-does-not-exist";
        Parameters params = parameters(part("artifactAssessmentType", new CodeType(artifactAssessmentType)));
        UnprocessableEntityException maybeException = null;
        var releaseVisitor = new ApproveVisitor(repo);
        Library lib = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(lib);

        try {
            libraryAdapter.accept(releaseVisitor, params);
        } catch (UnprocessableEntityException e) {
            maybeException = e;
        }
        assertNotNull(maybeException);
    }

    @Test
    void approveOperation_test() {
        var practitioner = (Practitioner)
                jsonParser.parseResource(ReleaseVisitorTests.class.getResourceAsStream("Practitioner-minimal.json"));
        repo.update(practitioner);
        Date today = new Date();
        // get today's date in the form "2023-05-11"
        DateType approvalDate = new DateType(today, TemporalPrecisionEnum.DAY);
        String artifactAssessmentType = "comment";
        String artifactAssessmentSummary = "comment text";
        String artifactAssessmentTarget = "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|1.0.0";
        String artifactAssessmentRelatedArtifact = "reference-valid-no-spaces";
        String artifactAssessmentAuthor = "Practitioner/sample-practitioner";
        Parameters params = parameters(
                part("approvalDate", approvalDate),
                part("artifactAssessmentType", new CodeType(artifactAssessmentType)),
                part("artifactAssessmentSummary", new MarkdownType(artifactAssessmentSummary)),
                part("artifactAssessmentTarget", new CanonicalType(artifactAssessmentTarget)),
                part("artifactAssessmentRelatedArtifact", new CanonicalType(artifactAssessmentRelatedArtifact)),
                part("artifactAssessmentAuthor", new Reference(artifactAssessmentAuthor)));
        var approveVisitor = new ApproveVisitor(repo);
        Library lib = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(lib);
        Bundle returnedResource = (Bundle) libraryAdapter.accept(approveVisitor, params);

        assertNotNull(returnedResource);
        Library approvedLibrary = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        assertNotNull(approvedLibrary);
        // Ensure Approval Date matches input parameter
        assertEquals(approvedLibrary.getApprovalDateElement().asStringValue(), approvalDate.asStringValue());
        // Ensure that approval date is NOT before Library.date (see $release)
        assertFalse(approvedLibrary.getApprovalDate().before(approvedLibrary.getDate()));
        // ArtifactAssessment is saved as type Basic, update when we change to OperationOutcome
        // Get the reference from BundleEntry.response.location
        Optional<BundleEntryComponent> maybeArtifactAssessment = returnedResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Basic"))
                .findAny();
        assertTrue(maybeArtifactAssessment.isPresent());
        var basic = repo.read(
                Basic.class,
                new IdType(maybeArtifactAssessment.get().getResponse().getLocation()));
        ArtifactAssessment artifactAssessment = new ArtifactAssessment();
        artifactAssessment.setExtension(basic.getExtension());
        artifactAssessment.setId(basic.getClass().getSimpleName() + "/" + basic.getIdPart());
        assertNotNull(artifactAssessment);
        assertTrue(artifactAssessment.isValidArtifactComment());
        assertTrue(artifactAssessment.checkArtifactCommentParams(
                artifactAssessmentType,
                artifactAssessmentSummary,
                "Library/SpecificationLibrary",
                artifactAssessmentRelatedArtifact,
                artifactAssessmentTarget,
                artifactAssessmentAuthor));
    }
}
