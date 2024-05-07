package org.opencds.cqf.fhir.utility.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.MarkdownType;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentContentClassifier;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentContentClassifierEnumFactory;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentContentExtension;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentContentInformationType;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentContentInformationTypeEnumFactory;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentDisposition;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentDispositionEnumFactory;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentWorkflowStatus;
import org.opencds.cqf.fhir.utility.dstu3.ArtifactAssessment.ArtifactAssessmentWorkflowStatusEnumFactory;

class ArtifactAssessmentTest {
    @Test
    void constructors() {
        var referenceString = "Library/123";
        ArtifactAssessment artifactAssessment = new ArtifactAssessment(new Reference(referenceString));
        assertEquals(
                ((Reference) artifactAssessment
                                .getExtensionByUrl(ArtifactAssessment.ARTIFACT)
                                .getValue())
                        .getReference(),
                referenceString);
        artifactAssessment = new ArtifactAssessment(new UriType(referenceString));
        assertEquals(
                ((UriType) artifactAssessment
                                .getExtensionByUrl(ArtifactAssessment.ARTIFACT)
                                .getValue())
                        .getValue(),
                referenceString);
    }

    @Test
    void code() {

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
                assertEquals(infoType.getSystem(), infoTypeSystem);
                assertEquals(infoType.toCode(), infoTypeCodes.get(i));
                assertEquals(infoType.getDefinition(), infoTypeDefinitions.get(i));
                assertEquals(infoType.getDisplay(), infoTypeDisplays.get(i));
                assertEquals(
                        new ArtifactAssessmentContentInformationTypeEnumFactory().toCode(infoType),
                        infoTypeCodes.get(i));
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
            assertEquals("", invalidCodeException);
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
                assertEquals(classifier.getSystem(), contentClassifierSystem);
                assertEquals(classifier.toCode(), contentClassifierCodes.get(i));
                assertEquals(classifier.getDefinition(), contentClassifierDefinitions.get(i));
                assertEquals(classifier.getDisplay(), contentClassifierDisplays.get(i));
                assertEquals(
                        new ArtifactAssessmentContentClassifierEnumFactory().toCode(classifier),
                        contentClassifierCodes.get(i));
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
            assertEquals("", invalidCodeException);
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
                assertEquals(workflowStatus.getSystem(), workflowStatusSystem);
                assertEquals(workflowStatus.toCode(), workflowStatusCodes.get(i));
                assertEquals(workflowStatus.getDefinition(), workflowStatusDefinitions.get(i));
                assertEquals(workflowStatus.getDisplay(), workflowStatusDisplays.get(i));
                assertEquals(
                        new ArtifactAssessmentWorkflowStatusEnumFactory().toCode(workflowStatus),
                        workflowStatusCodes.get(i));
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
            assertEquals("", invalidCodeException);
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
                assertEquals(disposition.getSystem(), dispositionSystem);
                assertEquals(disposition.toCode(), dispositionCodes.get(i));
                assertEquals(disposition.getDefinition(), dispositionDefinitions.get(i));
                assertEquals(disposition.getDisplay(), dispositionDisplays.get(i));
                assertEquals(
                        new ArtifactAssessmentDispositionEnumFactory().toCode(disposition), dispositionCodes.get(i));
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
            assertEquals("", invalidCodeException);
        });
    }

    @Test
    void valid_comment() {
        var referenceString = "Library/123";
        ArtifactAssessment artifactAssessment = new ArtifactAssessment();
        artifactAssessment.createArtifactComment(
                "comment",
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
                "comment",
                new Reference((String) null),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null),
                Optional.ofNullable(null));
        assertFalse(artifactAssessment.isValidArtifactComment());
    }

    @Test
    void setters() {
        ArtifactAssessment artifactAssessment = new ArtifactAssessment();
        var testDate = new Date();
        artifactAssessment.setApprovalDateExtension(new DateType(testDate));
        assertEquals(
                ((DateType) artifactAssessment
                                .getExtensionByUrl(ArtifactAssessment.APPROVAL_DATE)
                                .getValue())
                        .getValue(),
                testDate);
        var testReviewDate = new Date();
        artifactAssessment.setLastReviewDateExtension(new DateType(testReviewDate));
        assertEquals(
                ((DateType) artifactAssessment
                                .getExtensionByUrl(ArtifactAssessment.LAST_REVIEW_DATE)
                                .getValue())
                        .getValue(),
                testReviewDate);
        var testApprovalDate = new Date();
        artifactAssessment.setApprovalDateExtension(new DateType(testApprovalDate));
        assertEquals(
                ((DateType) artifactAssessment
                                .getExtensionByUrl(ArtifactAssessment.APPROVAL_DATE)
                                .getValue())
                        .getValue(),
                testApprovalDate);
        var title = new MarkdownType("title");
        artifactAssessment.setTitleExtension(title);
        assertEquals(
                ((MarkdownType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.TITLE)
                        .getValue()),
                title);
        var copyright = new MarkdownType("copyright");
        artifactAssessment.setCopyrightExtension(copyright);
        assertEquals(
                ((MarkdownType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.COPYRIGHT)
                        .getValue()),
                copyright);
        var citeAs = new MarkdownType("citeAs");
        artifactAssessment.setCiteAsExtension(citeAs);
        assertEquals(
                ((MarkdownType) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.CITEAS)
                        .getValue()),
                citeAs);
        var citeAsRef = new Reference("Library/citeAs");
        artifactAssessment.setCiteAsExtension(citeAsRef);
        assertEquals(
                ((Reference) artifactAssessment
                        .getExtensionByUrl(ArtifactAssessment.CITEAS)
                        .getValue()),
                citeAsRef);
        var workflowStatusCode = ArtifactAssessmentWorkflowStatus.PUBLISHED;
        var workflowStatus = new Enumeration<ArtifactAssessmentWorkflowStatus>(
                new ArtifactAssessment.ArtifactAssessmentWorkflowStatusEnumFactory());
        workflowStatus.setValue(workflowStatusCode);
        artifactAssessment.setArtifactAssessmentWorkflowStatusExtension(workflowStatus);
        assertEquals(
                ((Enumeration<ArtifactAssessmentWorkflowStatus>) artifactAssessment
                                .getExtensionByUrl(ArtifactAssessment.WORKFLOW_STATUS)
                                .getValue())
                        .getValue(),
                workflowStatusCode);
        var dispositionCode = ArtifactAssessmentDisposition.PERSUASIVE;
        var disposition = new Enumeration<ArtifactAssessmentDisposition>(
                new ArtifactAssessment.ArtifactAssessmentDispositionEnumFactory());
        disposition.setValue(dispositionCode);
        artifactAssessment.setArtifactAssessmentDispositionExtension(disposition);
        assertEquals(
                ((Enumeration<ArtifactAssessmentDisposition>) artifactAssessment
                                .getExtensionByUrl(ArtifactAssessment.DISPOSITION)
                                .getValue())
                        .getValue(),
                dispositionCode);
        var contentExtension = artifactAssessment.new ArtifactAssessmentContentExtension();
        artifactAssessment.addExtension(contentExtension);
        var authorReference = new Reference("Practitioner/author");
        contentExtension.setAuthorExtension(authorReference);
        Optional<Extension> contentAuthorExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.AUTHOR));
        assertTrue(contentAuthorExtension.isPresent());
        assertEquals(((Reference) contentAuthorExtension.get().getValue()), authorReference);
        var summary = new MarkdownType("summary");
        contentExtension.setSummary(summary);
        Optional<Extension> contentSummaryExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.SUMMARY));
        assertTrue(contentSummaryExtension.isPresent());
        assertEquals(((MarkdownType) contentSummaryExtension.get().getValue()), summary);
        var relatedArtifactCanonical = new UriType("canonical");
        contentExtension.addRelatedArtifact(relatedArtifactCanonical, RelatedArtifactType.CITATION);
        Optional<Extension> contentRelatedArtifactExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.RELATEDARTIFACT));
        assertTrue(contentRelatedArtifactExtension.isPresent());
        assertEquals(
                ((RelatedArtifact) contentRelatedArtifactExtension.get().getValue())
                        .getResource()
                        .getReference(),
                relatedArtifactCanonical.getValue());
        var quantity = new Quantity(4);
        contentExtension.setQuantityExtension(quantity);
        Optional<Extension> contentQuantityExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.QUANTITY));
        assertTrue(contentQuantityExtension.isPresent());
        assertEquals(((Quantity) contentQuantityExtension.get().getValue()), quantity);
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
        assertEquals(((CodeableConcept) contentTypeExtension.get().getValue()), type);
        var freeToShare = new BooleanType(true);
        contentExtension.setFreeToShareExtension(freeToShare);
        Optional<Extension> contentFreeToShareExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.FREETOSHARE));
        assertTrue(contentFreeToShareExtension.isPresent());
        assertEquals(((BooleanType) contentFreeToShareExtension.get().getValue()), freeToShare);
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
        assertEquals(((CodeableConcept) contentClassifierExtension.get().getValue()), classifer);
        var path = new UriType("path/test/thing");
        contentExtension.addPathExtension(path);
        Optional<Extension> contentPathExtension = Optional.of(
                        artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT))
                .map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.PATH));
        assertTrue(contentPathExtension.isPresent());
        assertEquals(((UriType) contentPathExtension.get().getValue()), path);

        var derivedFrom = new UriType("Library/derived-from");
        artifactAssessment.setDerivedFromContentRelatedArtifact(derivedFrom);
        var derivedFromContentRelatedArtifact = artifactAssessment.getDerivedFromContentRelatedArtifact();
        assertTrue(derivedFromContentRelatedArtifact.isPresent());
        assertEquals(derivedFromContentRelatedArtifact.get().getResource().getReference(), derivedFrom.getValue());
    }
}
