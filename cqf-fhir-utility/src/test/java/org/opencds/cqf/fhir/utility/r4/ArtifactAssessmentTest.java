package org.opencds.cqf.fhir.utility.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentInformationType;

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
    }
}
