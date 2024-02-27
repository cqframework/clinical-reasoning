package org.opencds.cqf.fhir.utility.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Optional;

import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationTypeEnumFactory;
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
    }
}
