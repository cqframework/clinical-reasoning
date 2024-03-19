package org.opencds.cqf.fhir.utility.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentClassifier;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentClassifierEnumFactory;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentExtension;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationTypeEnumFactory;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentDisposition;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentDispositionEnumFactory;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentWorkflowStatus;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentWorkflowStatusEnumFactory;

public class ArtifactAssessmentTest {
    @Test
    void test_constructors() {
        var referenceString = "Library/123";
        ArtifactAssessment artifactAssessment = new ArtifactAssessment(new Reference(referenceString));
        assertTrue(((Reference) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.ARTIFACT)
                        .getValue())
                .getReference()
                .equals(referenceString));
        artifactAssessment = new ArtifactAssessment(new UriType(referenceString));
        assertTrue(((UriType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.ARTIFACT)
                        .getValue())
                .getValue()
                .equals(referenceString));
        artifactAssessment = new ArtifactAssessment(new CanonicalType(referenceString));
        assertTrue(((CanonicalType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.ARTIFACT)
                        .getValue())
                .getValue()
                .equals(referenceString));
    }

    @Test
    void test_code() {

        var infoTypeSystem = "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
        var infoTypeCodes = List.of("comment", "classifier", "rating", "container", "response", "change-request");
        var infoTypeDefinitions = List.of(
                "A comment on the artifact.",
                "A classifier of the artifact.",
                "A rating  of the artifact.",
                "A container for multiple components.",
                "A response to a comment.",
                "A change request for the artifact.");
        var infoTypeDisplays = List.of("Comment", "Classifier", "Rating", "Container", "Response", "Change Request");
        var infoTypesList = Arrays.asList(ArtifactAssessmentContentInformationType.values());
        for (var i = 0; i < infoTypesList.size(); i++) {
            var infoType = infoTypesList.get(i);
            if (infoType != ArtifactAssessmentContentInformationType.NULL) {
                assertTrue(infoType.getSystem().equals(infoTypeSystem));
                assertTrue(infoType.toCode().equals(infoTypeCodes.get(i)));
                assertTrue(infoType.getDefinition().equals(infoTypeDefinitions.get(i)));
                assertTrue(infoType.getDisplay().equals(infoTypeDisplays.get(i)));
                assertTrue(new ArtifactAssessmentContentInformationTypeEnumFactory()
                        .toCode(infoType)
                        .equals(infoTypeCodes.get(i)));
            }
        }
        infoTypeCodes.forEach(code -> {
            var invalidCodeException = "";
            try {
                ArtifactAssessmentContentInformationType.fromCode(code);
                new ArtifactAssessmentContentInformationTypeEnumFactory().fromCode(code);
                new ArtifactAssessmentContentInformationTypeEnumFactory().fromType(new StringType(code));
            } catch (FHIRException e) {
                invalidCodeException = e.getMessage();
            }
            assertTrue(invalidCodeException.equals(""));
        });
        var contentClassifierCodes = List.of(
                "high",
                "moderate",
                "low",
                "very-low",
                "no-concern",
                "serious-concern",
                "very-serious-concern",
                "extremely-serious-concern",
                "present",
                "absent",
                "no-change",
                "downcode1",
                "downcode2",
                "downcode3",
                "upcode1",
                "upcode2");
        var contentClassifierDefinitions = List.of(
                "High quality evidence.",
                "Moderate quality evidence.",
                "Low quality evidence.",
                "Very low quality evidence.",
                "No serious concern.",
                "Serious concern.",
                "Very serious concern.",
                "Extremely serious concern.",
                "Possible reason for increasing quality rating was checked and found to be present.",
                "Possible reason for increasing quality rating was checked and found to be absent.",
                "No change to quality rating.",
                "Reduce quality rating by 1.",
                "Reduce quality rating by 2.",
                "Reduce quality rating by 3.",
                "Increase quality rating by 1.",
                "Increase quality rating by 2.");
        var contentClassifierDisplays = List.of(
                "High quality",
                "Moderate quality",
                "Low quality",
                "Very low quality",
                "No serious concern",
                "Serious concern",
                "Very serious concern",
                "Extremely serious concern",
                "Present",
                "Absent",
                "No change to rating",
                "Reduce rating: -1",
                "Reduce rating: -2",
                "Reduce rating: -3",
                "Increase rating: +1",
                "Increase rating: +2");
        var contentClassifierSystem = "http://terminology.hl7.org/CodeSystem/certainty-rating";
        var contentClassifiersList = Arrays.asList(ArtifactAssessmentContentClassifier.values());
        for (var i = 0; i < contentClassifiersList.size(); i++) {
            var classifier = contentClassifiersList.get(i);
            if (classifier != ArtifactAssessmentContentClassifier.NULL) {
                assertTrue(classifier.getSystem().equals(contentClassifierSystem));
                assertTrue(classifier.toCode().equals(contentClassifierCodes.get(i)));
                assertTrue(classifier.getDefinition().equals(contentClassifierDefinitions.get(i)));
                assertTrue(classifier.getDisplay().equals(contentClassifierDisplays.get(i)));
                assertTrue(new ArtifactAssessmentContentClassifierEnumFactory()
                        .toCode(classifier)
                        .equals(contentClassifierCodes.get(i)));
            }
        }
        contentClassifierCodes.forEach(code -> {
            var invalidCodeException = "";
            try {
                ArtifactAssessmentContentClassifier.fromCode(code);
                new ArtifactAssessmentContentClassifierEnumFactory().fromCode(code);
                new ArtifactAssessmentContentClassifierEnumFactory().fromType(new StringType(code));
            } catch (FHIRException e) {
                invalidCodeException = e.getMessage();
            }
            assertTrue(invalidCodeException.equals(""));
        });
        var workflowStatusSystem = "http://hl7.org/fhir/artifactassessment-workflow-status";
        var workflowStatusCodes = List.of(
                "submitted",
                "triaged",
                "waiting-for-input",
                "resolved-no-change",
                "resolved-change-required",
                "deferred",
                "duplicate",
                "applied",
                "published");
        var workflowStatusDefinitions = List.of(
                "The comment has been submitted, but the responsible party has not yet been determined, or the responsible party has not yet determined the next steps to be taken.",
                "The comment has been triaged, meaning the responsible party has been determined and next steps have been identified to address the comment.",
                "The comment is waiting for input from a specific party before next steps can be taken.",
                "The comment has been resolved and no changes resulted from the resolution",
                "The comment has been resolved and changes are required to address the comment",
                "The comment is acceptable, but resolution of the comment and application of any associated changes have been deferred",
                "The comment is a duplicate of another comment already received",
                "The comment is resolved and any necessary changes have been applied",
                "The necessary changes to the artifact have been published in a new version of the artifact");
        var workflowStatusDisplays = List.of(
                "Submitted",
                "Triaged",
                "Waiting for Input",
                "Resolved - No Change",
                "Resolved - Change Required",
                "Deferred",
                "Duplicate",
                "Applied",
                "Published");
        var workflowStatusesList = Arrays.asList(ArtifactAssessmentWorkflowStatus.values());
        for (var i = 0; i < workflowStatusesList.size(); i++) {
            var workflowStatus = workflowStatusesList.get(i);
            if (workflowStatus != ArtifactAssessmentWorkflowStatus.NULL) {
                assertTrue(workflowStatus.getSystem().equals(workflowStatusSystem));
                assertTrue(workflowStatus.toCode().equals(workflowStatusCodes.get(i)));
                assertTrue(workflowStatus.getDefinition().equals(workflowStatusDefinitions.get(i)));
                assertTrue(workflowStatus.getDisplay().equals(workflowStatusDisplays.get(i)));
                assertTrue(new ArtifactAssessmentWorkflowStatusEnumFactory()
                        .toCode(workflowStatus)
                        .equals(workflowStatusCodes.get(i)));
            }
        }
        workflowStatusCodes.forEach(code -> {
            var invalidCodeException = "";
            try {
                ArtifactAssessmentWorkflowStatus.fromCode(code);
                new ArtifactAssessmentWorkflowStatusEnumFactory().fromCode(code);
                new ArtifactAssessmentWorkflowStatusEnumFactory().fromType(new StringType(code));
            } catch (FHIRException e) {
                invalidCodeException = e.getMessage();
            }
            assertTrue(invalidCodeException.equals(""));
        });
        var dispositionCodes = List.of(
                "unresolved",
                "not-persuasive",
                "persuasive",
                "persuasive-with-modification",
                "not-persuasive-with-modification");
        var dispositionDefinitions = List.of(
                "The comment is unresolved",
                "The comment is not persuasive (rejected in full)",
                "The comment is persuasive (accepted in full)",
                "The comment is persuasive with modification (partially accepted)",
                "The comment is not persuasive with modification (partially rejected)");
        var dispositionDisplays = List.of(
                "Unresolved",
                "Not Persuasive",
                "Persuasive",
                "Persuasive with Modification",
                "Not Persuasive with Modification");
        var dispositionSystem = "http://hl7.org/fhir/artifactassessment-disposition";
        var dispositionsList = Arrays.asList(ArtifactAssessmentDisposition.values());
        for (var i = 0; i < dispositionsList.size(); i++) {
            var disposition = dispositionsList.get(i);
            if (disposition != ArtifactAssessmentDisposition.NULL) {
                assertTrue(disposition.getSystem().equals(dispositionSystem));
                assertTrue(disposition.toCode().equals(dispositionCodes.get(i)));
                assertTrue(disposition.getDefinition().equals(dispositionDefinitions.get(i)));
                assertTrue(disposition.getDisplay().equals(dispositionDisplays.get(i)));
                assertTrue(new ArtifactAssessmentDispositionEnumFactory()
                        .toCode(disposition)
                        .equals(dispositionCodes.get(i)));
            }
        }
        dispositionCodes.forEach(code -> {
            var invalidCodeException = "";
            try {
                ArtifactAssessmentDisposition.fromCode(code);
                new ArtifactAssessmentDispositionEnumFactory().fromCode(code);
            } catch (FHIRException e) {
                invalidCodeException = e.getMessage();
            }
            assertTrue(invalidCodeException.equals(""));
        });
    }

    @Test
    void test_valid_comment() {
        var referenceString = "Library/123";
        ArtifactAssessment artifactAssessment = new ArtifactAssessment();
        artifactAssessment.createArtifactComment(
                ArtifactAssessmentContentInformationType.fromCode("comment"),
                new Reference(referenceString),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null));
        assertTrue(artifactAssessment.isValidArtifactComment());
        artifactAssessment = new ArtifactAssessment();
        artifactAssessment.createArtifactComment(
                null,
                new Reference(referenceString),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null));
        assertFalse(artifactAssessment.isValidArtifactComment());
        artifactAssessment = new ArtifactAssessment();
        artifactAssessment.createArtifactComment(
                ArtifactAssessmentContentInformationType.fromCode("comment"),
                new Reference((String) null),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null));
        assertFalse(artifactAssessment.isValidArtifactComment());
    }

    @Test
    void test_setters() {
        ArtifactAssessment artifactAssessment = new ArtifactAssessment();
        var testDate = new Date();
        artifactAssessment.setApprovalDateExtension(new DateType(testDate));
        assertTrue(((DateType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.APPROVAL_DATE)
                        .getValue())
                .getValue()
                .equals(testDate));
        var testReviewDate = new Date();
        artifactAssessment.setLastReviewDateExtension(new DateType(testReviewDate));
        assertTrue(((DateType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.LAST_REVIEW_DATE)
                        .getValue())
                .getValue()
                .equals(testReviewDate));
        var testApprovalDate = new Date();
        artifactAssessment.setApprovalDateExtension(new DateType(testApprovalDate));
        assertTrue(((DateType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.APPROVAL_DATE)
                        .getValue())
                .getValue()
                .equals(testApprovalDate));
        var title = new MarkdownType("title");
        artifactAssessment.setTitleExtension(title);
        assertTrue(((MarkdownType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.TITLE)
                        .getValue())
                .equals(title));
        var copyright = new MarkdownType("copyright");
        artifactAssessment.setCopyrightExtension(copyright);
        assertTrue(((MarkdownType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.COPYRIGHT)
                        .getValue())
                .equals(copyright));
        var citeAs = new MarkdownType("citeAs");
        artifactAssessment.setCiteAsExtension(citeAs);
        assertTrue(((MarkdownType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.CITEAS)
                        .getValue())
                .equals(citeAs));
        var citeAsRef = new Reference("Library/citeAs");
        artifactAssessment.setCiteAsExtension(citeAsRef);
        assertTrue(((Reference) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.CITEAS)
                        .getValue())
                .equals(citeAsRef));
        var workflowStatusCode = ArtifactAssessmentWorkflowStatus.PUBLISHED;
        var workflowStatus = new Enumeration<ArtifactAssessmentWorkflowStatus>(
                new ArtifactAssessment.ArtifactAssessmentWorkflowStatusEnumFactory());
        workflowStatus.setValue(workflowStatusCode);
        artifactAssessment.setArtifactAssessmentWorkflowStatusExtension(workflowStatus);
        assertTrue(((Enumeration<ArtifactAssessmentWorkflowStatus>) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.WORKFLOW_STATUS)
                        .getValue())
                .getValue()
                .equals(workflowStatusCode));
        var dispositionCode = ArtifactAssessmentDisposition.PERSUASIVE;
        var disposition = new Enumeration<ArtifactAssessmentDisposition>(
                new ArtifactAssessment.ArtifactAssessmentDispositionEnumFactory());
        disposition.setValue(dispositionCode);
        artifactAssessment.setArtifactAssessmentDispositionExtension(disposition);
        assertTrue(((Enumeration<ArtifactAssessmentDisposition>) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.DISPOSITION)
                        .getValue())
                .getValue()
                .equals(dispositionCode));
        var contentExtension = artifactAssessment.new ArtifactAssessmentContentExtension();
        artifactAssessment.addExtension(contentExtension);
        var authorReference = new Reference("Practitioner/author");
        contentExtension.setAuthorExtension(authorReference);
        Optional<Extension> contentAuthorExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.AUTHOR));
        assertTrue(contentAuthorExtension.isPresent());
        assertTrue(((Reference) contentAuthorExtension.get().getValue()).equals(authorReference));
        var summary = new MarkdownType("summary");
        contentExtension.setSummary(summary);
        Optional<Extension> contentSummaryExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.SUMMARY));
        assertTrue(contentSummaryExtension.isPresent());
        assertTrue(((MarkdownType) contentSummaryExtension.get().getValue()).equals(summary));
        var relatedArtifactCanonical = new CanonicalType("canonical");
        contentExtension.addRelatedArtifact(relatedArtifactCanonical, RelatedArtifactType.CITATION);
        Optional<Extension> contentRelatedArtifactExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.RELATEDARTIFACT));
        assertTrue(contentRelatedArtifactExtension.isPresent());
        assertTrue(((RelatedArtifact) contentRelatedArtifactExtension.get().getValue())
                .getResourceElement()
                .equals(relatedArtifactCanonical));
        var quantity = new Quantity(4);
        contentExtension.setQuantityExtension(quantity);
        Optional<Extension> contentQuantityExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.QUANTITY));
        assertTrue(contentQuantityExtension.isPresent());
        assertTrue(((Quantity) contentQuantityExtension.get().getValue()).equals(quantity));
        var typeCoding = new Coding(
                "http://hl7.org/fhir/ValueSet/certainty-type",
                "LargeEffect",
                "higher certainty due to large effect size");
        var type = new CodeableConcept();
        type.addCoding(typeCoding);
        contentExtension.setTypeExtension(type);
        Optional<Extension> contentTypeExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.TYPE));
        assertTrue(contentTypeExtension.isPresent());
        assertTrue(((CodeableConcept) contentTypeExtension.get().getValue()).equals(type));
        var freeToShare = new BooleanType(true);
        contentExtension.setFreeToShareExtension(freeToShare);
        Optional<Extension> contentFreeToShareExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.FREETOSHARE));
        assertTrue(contentFreeToShareExtension.isPresent());
        assertTrue(((BooleanType) contentFreeToShareExtension.get().getValue()).equals(freeToShare));
        var classifierCoding = new Coding(
                "http://hl7.org/fhir/ValueSet/certainty-type",
                "LargeEffect",
                "higher certainty due to large effect size");
        var classifer = new CodeableConcept();
        classifer.addCoding(classifierCoding);
        contentExtension.addClassifierExtension(classifer);
        Optional<Extension> contentClassifierExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.CLASSIFIER));
        assertTrue(contentClassifierExtension.isPresent());
        assertTrue(((CodeableConcept) contentClassifierExtension.get().getValue()).equals(classifer));
        var path = new UriType("path/test/thing");
        contentExtension.addPathExtension(path);
        Optional<Extension> contentPathExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.PATH));
        assertTrue(contentPathExtension.isPresent());
        assertTrue(((UriType) contentPathExtension.get().getValue()).equals(path));

        var derivedFrom = new CanonicalType("Library/derived-from");
        artifactAssessment.setDerivedFromContentRelatedArtifact(derivedFrom);
        var derivedFromContentRelatedArtifact = artifactAssessment.getDerivedFromContentRelatedArtifact();
        assertTrue(derivedFromContentRelatedArtifact.isPresent());
        assertTrue(derivedFromContentRelatedArtifact.get().getResourceElement().equals(derivedFrom));
    }
}
