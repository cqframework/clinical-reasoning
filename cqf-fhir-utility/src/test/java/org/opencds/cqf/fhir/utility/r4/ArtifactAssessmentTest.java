package org.opencds.cqf.fhir.utility.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Optional;

import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentClassifier;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentExtension;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentDisposition;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentWorkflowStatus;

public class ArtifactAssessmentTest {
    @Test
    void test_constructors() {
        var referenceString = "Library/123";
        ArtifactAssessment artifactAssessment = new ArtifactAssessment(new Reference(referenceString));
        assertTrue(((Reference)artifactAssessment.getExtensionByUrl(ArtifactAssessment.ARTIFACT).getValue()).getReference().equals(referenceString));
        artifactAssessment = new ArtifactAssessment(new UriType(referenceString));
        assertTrue(((UriType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.ARTIFACT).getValue()).getValue().equals(referenceString));
        artifactAssessment = new ArtifactAssessment(new CanonicalType(referenceString));
        assertTrue(((CanonicalType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.ARTIFACT).getValue()).getValue().equals(referenceString));
    }
    @Test
    void test_code () {
        var invalidCodeException = "";
        try {
            ArtifactAssessmentContentInformationType.fromCode("comment");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentInformationType.fromCode("classifier");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentInformationType.fromCode("rating");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentInformationType.fromCode("change-request");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentInformationType.fromCode("response");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentInformationType.fromCode("this-is-not-a-code");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.contains("Unknown"));
        // reset
        invalidCodeException = "";
        try {
            ArtifactAssessmentContentClassifier.fromCode("high");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("moderate");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("low");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("no-concern");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("serious-concern");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("very-serious-concern");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("extremely-serious-concern");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("present");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("absent");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("no-change");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("downcode1");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("downcode2");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("downcode3");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("upcode1");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
        try {
            ArtifactAssessmentContentClassifier.fromCode("upcode2");
        } catch (FHIRException e) {
            invalidCodeException = e.getMessage();
        }
        assertTrue(invalidCodeException.equals(""));
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
            new Reference((String)null), 
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
        assertTrue(((DateType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.APPROVAL_DATE).getValue()).getValue().equals(testDate));
        var testReviewDate = new Date();
        artifactAssessment.setLastReviewDateExtension(new DateType(testReviewDate));
        assertTrue(((DateType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.LAST_REVIEW_DATE).getValue()).getValue().equals(testReviewDate));
        var testApprovalDate = new Date();
        artifactAssessment.setApprovalDateExtension(new DateType(testApprovalDate));
        assertTrue(((DateType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.APPROVAL_DATE).getValue()).getValue().equals(testApprovalDate));
        var title = new MarkdownType("title");
        artifactAssessment.setTitleExtension(title);
        assertTrue(((MarkdownType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.TITLE).getValue()).equals(title));
        var copyright = new MarkdownType("copyright");
        artifactAssessment.setCopyrightExtension(copyright);
        assertTrue(((MarkdownType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.COPYRIGHT).getValue()).equals(copyright));
        var citeAs = new MarkdownType("citeAs");
        artifactAssessment.setCiteAsExtension(citeAs);
        assertTrue(((MarkdownType)artifactAssessment.getExtensionByUrl(ArtifactAssessment.CITEAS).getValue()).equals(citeAs));
        var citeAsRef = new Reference("Library/citeAs");
        artifactAssessment.setCiteAsExtension(citeAsRef);
        assertTrue(((Reference)artifactAssessment.getExtensionByUrl(ArtifactAssessment.CITEAS).getValue()).equals(citeAsRef));
        var workflowStatusCode = ArtifactAssessmentWorkflowStatus.PUBLISHED;
        var workflowStatus = new Enumeration<ArtifactAssessmentWorkflowStatus>(new ArtifactAssessment.ArtifactAssessmentWorkflowStatusEnumFactory());
        workflowStatus.setValue(workflowStatusCode);
        artifactAssessment.setArtifactAssessmentWorkflowStatusExtension(workflowStatus);
        assertTrue(((Enumeration<ArtifactAssessmentWorkflowStatus>)artifactAssessment.getExtensionByUrl(ArtifactAssessment.WORKFLOW_STATUS).getValue()).getValue().equals(workflowStatusCode));
        var dispositionCode = ArtifactAssessmentDisposition.PERSUASIVE;
        var disposition = new Enumeration<ArtifactAssessmentDisposition>(new ArtifactAssessment.ArtifactAssessmentDispositionEnumFactory());
        disposition.setValue(dispositionCode);
        artifactAssessment.setArtifactAssessmentDispositionExtension(disposition);
        assertTrue(((Enumeration<ArtifactAssessmentDisposition>)artifactAssessment.getExtensionByUrl(ArtifactAssessment.DISPOSITION).getValue()).getValue().equals(dispositionCode));
        var contentExtension = artifactAssessment.new ArtifactAssessmentContentExtension();
        artifactAssessment.addExtension(contentExtension);
        var authorReference = new Reference("Practitioner/author");
        contentExtension.setAuthorExtension(authorReference);
        Optional<Extension> contentAuthorExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.AUTHOR));
        assertTrue(contentAuthorExtension.isPresent());
        assertTrue(((Reference)contentAuthorExtension.get().getValue()).equals(authorReference));
        var summary = new MarkdownType("summary");
        contentExtension.setSummary(summary);
        Optional<Extension> contentSummaryExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.SUMMARY));
        assertTrue(contentSummaryExtension.isPresent());
        assertTrue(((MarkdownType)contentSummaryExtension.get().getValue()).equals(summary));
        var relatedArtifactCanonical = new CanonicalType("canonical");
        contentExtension.addRelatedArtifact(relatedArtifactCanonical, RelatedArtifactType.CITATION);
        Optional<Extension> contentRelatedArtifactExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.RELATEDARTIFACT));
        assertTrue(contentRelatedArtifactExtension.isPresent());
        assertTrue(((RelatedArtifact)contentRelatedArtifactExtension.get().getValue()).getResourceElement().equals(relatedArtifactCanonical));
        var quantity = new Quantity(4);
        contentExtension.setQuantityExtension(quantity);
        Optional<Extension> contentQuantityExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.QUANTITY));
        assertTrue(contentQuantityExtension.isPresent());
        assertTrue(((Quantity)contentQuantityExtension.get().getValue()).equals(quantity));
        var typeCoding = new Coding("http://hl7.org/fhir/ValueSet/certainty-type", "LargeEffect", "higher certainty due to large effect size");
        var type = new CodeableConcept();
        type.addCoding(typeCoding);
        contentExtension.setTypeExtension(type);
        Optional<Extension> contentTypeExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.TYPE));
        assertTrue(contentTypeExtension.isPresent());
        assertTrue(((CodeableConcept)contentTypeExtension.get().getValue()).equals(type));
        var freeToShare = new BooleanType(true);
        contentExtension.setFreeToShareExtension(freeToShare);
        Optional<Extension> contentFreeToShareExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.FREETOSHARE));
        assertTrue(contentFreeToShareExtension.isPresent());
        assertTrue(((BooleanType)contentFreeToShareExtension.get().getValue()).equals(freeToShare));
        var classifierCoding = new Coding("http://hl7.org/fhir/ValueSet/certainty-type", "LargeEffect", "higher certainty due to large effect size");
        var classifer = new CodeableConcept();
        classifer.addCoding(classifierCoding);
        contentExtension.addClassifierExtension(classifer);
        Optional<Extension> contentClassifierExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.CLASSIFIER));
        assertTrue(contentClassifierExtension.isPresent());
        assertTrue(((CodeableConcept)contentClassifierExtension.get().getValue()).equals(classifer));
        var path = new UriType("path/test/thing");
        contentExtension.addPathExtension(path);
        Optional<Extension> contentPathExtension = Optional.of(artifactAssessment.getExtensionByUrl(ArtifactAssessment.CONTENT)).map(ext -> ext.getExtensionByUrl(ArtifactAssessmentContentExtension.PATH));
        assertTrue(contentPathExtension.isPresent());
        assertTrue(((UriType)contentPathExtension.get().getValue()).equals(path));
        
        var derivedFrom = new CanonicalType("Library/derived-from");
        artifactAssessment.setDerivedFromContentRelatedArtifact(derivedFrom);
        var derivedFromContentRelatedArtifact = artifactAssessment.getDerivedFromContentRelatedArtifact();
        assertTrue(derivedFromContentRelatedArtifact.isPresent());
        assertTrue(derivedFromContentRelatedArtifact.get().getResourceElement().equals(derivedFrom));
    }
}
