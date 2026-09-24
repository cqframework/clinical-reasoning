package org.opencds.cqf.fhir.utility.r5;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.Basic;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Configuration;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.DateType;
import org.hl7.fhir.r5.model.EnumFactory;
import org.hl7.fhir.r5.model.Enumeration;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.MarkdownType;
import org.hl7.fhir.r5.model.PrimitiveType;
import org.hl7.fhir.r5.model.Quantity;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;

@ResourceDef(
        id = "ArtifactAssessment",
        profile = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessment")
public class ArtifactAssessment extends Basic {
    public enum ArtifactAssessmentContentInformationType {
        /**
         * A comment on the artifact
         */
        COMMENT,
        /**
         * A classifier of the artifact
         */
        CLASSIFIER,
        /**
         * A rating of the artifact
         */
        RATING,
        /**
         * A container for multiple components
         */
        CONTAINER,
        /**
         * A response to a comment
         */
        RESPONSE,
        /**
         * A change request for the artifact
         */
        CHANGEREQUEST,
        /**
         * added to help the parsers with the generic types
         */
        NULL;

        public static ArtifactAssessment.ArtifactAssessmentContentInformationType fromCode(String codeString)
                throws FHIRException {
            if (StringUtils.isBlank(codeString)) {
                return null;
            }
            switch (codeString) {
                case "comment":
                    return COMMENT;
                case "classifier":
                    return CLASSIFIER;
                case "rating":
                    return RATING;
                case "container":
                    return CONTAINER;
                case "response":
                    return RESPONSE;
                case "change-request":
                    return CHANGEREQUEST;

                default:
                    if (Configuration.isAcceptInvalidEnums()) {
                        return null;
                    } else {
                        throw new FHIRException("Unknown ArtifactAssessment '" + codeString + "'");
                    }
            }
        }

        public String toCode() {
            switch (this) {
                case COMMENT:
                    return "comment";
                case CLASSIFIER:
                    return "classifier";
                case RATING:
                    return "rating";
                case CONTAINER:
                    return "container";
                case RESPONSE:
                    return "response";
                case CHANGEREQUEST:
                    return "change-request";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getSystem() {
            switch (this) {
                case COMMENT, CLASSIFIER, RATING, CONTAINER, RESPONSE, CHANGEREQUEST:
                    return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDefinition() {
            switch (this) {
                case COMMENT:
                    return "A comment on the artifact.";
                case CLASSIFIER:
                    return "A classifier of the artifact.";
                case RATING:
                    return "A rating  of the artifact.";
                case CONTAINER:
                    return "A container for multiple components.";
                case RESPONSE:
                    return "A response to a comment.";
                case CHANGEREQUEST:
                    return "A change request for the artifact.";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch (this) {
                case COMMENT:
                    return "Comment";
                case CLASSIFIER:
                    return "Classifier";
                case RATING:
                    return "Rating";
                case CONTAINER:
                    return "Container";
                case RESPONSE:
                    return "Response";
                case CHANGEREQUEST:
                    return "Change Request";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }
    }

    public static class ArtifactAssessmentContentInformationTypeEnumFactory
            implements EnumFactory<ArtifactAssessment.ArtifactAssessmentContentInformationType> {
        public ArtifactAssessment.ArtifactAssessmentContentInformationType fromCode(String codeString)
                throws IllegalArgumentException {
            return ArtifactAssessment.ArtifactAssessmentContentInformationType.fromCode(codeString);
        }

        public Enumeration<ArtifactAssessment.ArtifactAssessmentContentInformationType> fromType(Base code)
                throws FHIRException {
            if (code == null) return null;
            if (code.isEmpty()) return new Enumeration<>(this);
            return new Enumeration<>(
                    this,
                    ArtifactAssessment.ArtifactAssessmentContentInformationType.fromCode(
                            ((PrimitiveType<?>) code).asStringValue()));
        }

        public String toCode(ArtifactAssessment.ArtifactAssessmentContentInformationType code) {
            return code.toCode();
        }

        public String toSystem(ArtifactAssessment.ArtifactAssessmentContentInformationType code) {
            return code.getSystem();
        }
    }

    public enum ArtifactAssessmentContentClassifier {
        /**
         * High quality evidence.
         */
        HIGH,
        /**
         * Moderate quality evidence.
         */
        MODERATE,
        /**
         * Low quality evidence.
         */
        LOW,
        /**
         * Very low quality evidence
         */
        VERY_LOW,
        /**
         * No serious concern.
         */
        NO_CONCERN,
        /**
         * Serious concern.
         */
        SERIOUS_CONCERN,
        /**
         * Very serious concern.
         */
        VERY_SERIOUS_CONCERN,
        /**
         * Extremely serious concern.
         */
        EXTREMELY_SERIOUS_CONCERN,
        /**
         * Possible reason for increasing quality rating was checked and found to be present.
         */
        PRESENT,
        /**
         * Possible reason for increasing quality rating was checked and found to be absent.
         */
        ABSENT,
        /**
         * No change to quality rating.
         */
        NO_CHANGE,
        /**
         * Reduce quality rating by 1.
         */
        DOWNCODE1,
        /**
         * Reduce quality rating by 2.
         */
        DOWNCODE2,
        /**
         * Reduce quality rating by 3.
         */
        DOWNCODE3,
        /**
         * Increase quality rating by 1.
         */
        UPCODE1,
        /**
         * Increase quality rating by 2
         */
        UPCODE2,
        /**
         * added to help the parsers with the generic types
         */
        NULL;

        public static ArtifactAssessment.ArtifactAssessmentContentClassifier fromCode(String codeString)
                throws FHIRException {
            if (StringUtils.isBlank(codeString)) {
                return null;
            }
            switch (codeString) {
                case "high":
                    return HIGH;
                case "moderate":
                    return MODERATE;
                case "low":
                    return LOW;
                case "very-low":
                    return VERY_LOW;
                case "no-concern":
                    return NO_CONCERN;
                case "serious-concern":
                    return SERIOUS_CONCERN;
                case "very-serious-concern":
                    return VERY_SERIOUS_CONCERN;
                case "extremely-serious-concern":
                    return EXTREMELY_SERIOUS_CONCERN;
                case "present":
                    return PRESENT;
                case "absent":
                    return ABSENT;
                case "no-change":
                    return NO_CHANGE;
                case "downcode1":
                    return DOWNCODE1;
                case "downcode2":
                    return DOWNCODE2;
                case "downcode3":
                    return DOWNCODE3;
                case "upcode1":
                    return UPCODE1;
                case "upcode2":
                    return UPCODE2;
                default:
                    if (Configuration.isAcceptInvalidEnums()) {
                        return null;
                    } else {
                        throw new FHIRException("Unknown ArtifactAssessment '" + codeString + "'");
                    }
            }
        }

        public String toCode() {
            switch (this) {
                case HIGH:
                    return "high";
                case MODERATE:
                    return "moderate";
                case LOW:
                    return "low";
                case VERY_LOW:
                    return "very-low";
                case NO_CONCERN:
                    return "no-concern";
                case SERIOUS_CONCERN:
                    return "serious-concern";
                case VERY_SERIOUS_CONCERN:
                    return "very-serious-concern";
                case EXTREMELY_SERIOUS_CONCERN:
                    return "extremely-serious-concern";
                case PRESENT:
                    return "present";
                case ABSENT:
                    return "absent";
                case NO_CHANGE:
                    return "no-change";
                case DOWNCODE1:
                    return "downcode1";
                case DOWNCODE2:
                    return "downcode2";
                case DOWNCODE3:
                    return "downcode3";
                case UPCODE1:
                    return "upcode1";
                case UPCODE2:
                    return "upcode2";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getSystem() {
            switch (this) {
                case HIGH,
                        MODERATE,
                        LOW,
                        VERY_LOW,
                        NO_CONCERN,
                        SERIOUS_CONCERN,
                        VERY_SERIOUS_CONCERN,
                        EXTREMELY_SERIOUS_CONCERN,
                        PRESENT,
                        ABSENT,
                        NO_CHANGE,
                        DOWNCODE1,
                        DOWNCODE2,
                        DOWNCODE3,
                        UPCODE1,
                        UPCODE2:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDefinition() {
            switch (this) {
                case HIGH:
                    return "High quality evidence.";
                case MODERATE:
                    return "Moderate quality evidence.";
                case LOW:
                    return "Low quality evidence.";
                case VERY_LOW:
                    return "Very low quality evidence.";
                case NO_CONCERN:
                    return "No serious concern.";
                case SERIOUS_CONCERN:
                    return "Serious concern.";
                case VERY_SERIOUS_CONCERN:
                    return "Very serious concern.";
                case EXTREMELY_SERIOUS_CONCERN:
                    return "Extremely serious concern.";
                case PRESENT:
                    return "Possible reason for increasing quality rating was checked and found to be present.";
                case ABSENT:
                    return "Possible reason for increasing quality rating was checked and found to be absent.";
                case NO_CHANGE:
                    return "No change to quality rating.";
                case DOWNCODE1:
                    return "Reduce quality rating by 1.";
                case DOWNCODE2:
                    return "Reduce quality rating by 2.";
                case DOWNCODE3:
                    return "Reduce quality rating by 3.";
                case UPCODE1:
                    return "Increase quality rating by 1.";
                case UPCODE2:
                    return "Increase quality rating by 2.";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch (this) {
                case HIGH:
                    return "High quality";
                case MODERATE:
                    return "Moderate quality";
                case LOW:
                    return "Low quality";
                case VERY_LOW:
                    return "Very low quality";
                case NO_CONCERN:
                    return "No serious concern";
                case SERIOUS_CONCERN:
                    return "Serious concern";
                case VERY_SERIOUS_CONCERN:
                    return "Very serious concern";
                case EXTREMELY_SERIOUS_CONCERN:
                    return "Extremely serious concern";
                case PRESENT:
                    return "Present";
                case ABSENT:
                    return "Absent";
                case NO_CHANGE:
                    return "No change to rating";
                case DOWNCODE1:
                    return "Reduce rating: -1";
                case DOWNCODE2:
                    return "Reduce rating: -2";
                case DOWNCODE3:
                    return "Reduce rating: -3";
                case UPCODE1:
                    return "Increase rating: +1";
                case UPCODE2:
                    return "Increase rating: +2";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }
    }

    public static class ArtifactAssessmentContentClassifierEnumFactory
            implements EnumFactory<ArtifactAssessment.ArtifactAssessmentContentClassifier> {
        public ArtifactAssessment.ArtifactAssessmentContentClassifier fromCode(String codeString)
                throws IllegalArgumentException {
            return ArtifactAssessment.ArtifactAssessmentContentClassifier.fromCode(codeString);
        }

        public Enumeration<ArtifactAssessment.ArtifactAssessmentContentClassifier> fromType(Base code)
                throws FHIRException {
            if (code == null) {
                return null;
            }
            if (code.isEmpty()) {
                return new Enumeration<>(this);
            }
            return new Enumeration<>(
                    this,
                    ArtifactAssessment.ArtifactAssessmentContentClassifier.fromCode(
                            ((PrimitiveType<?>) code).asStringValue()));
        }

        public String toCode(ArtifactAssessment.ArtifactAssessmentContentClassifier code) {
            return code.toCode();
        }

        public String toSystem(ArtifactAssessment.ArtifactAssessmentContentClassifier code) {
            return code.getSystem();
        }
    }

    public enum ArtifactAssessmentContentType {}

    public enum ArtifactAssessmentWorkflowStatus {
        /**
         * The comment has been submitted, but the responsible party has not yet been determined, or the responsible party has not yet determined the next steps to be taken.
         */
        SUBMITTED,
        /**
         * The comment has been triaged, meaning the responsible party has been determined and next steps have been identified to address the comment.
         */
        TRIAGED,
        /**
         * The comment is waiting for input from a specific party before next steps can be taken.
         */
        WAITINGFORINPUT,
        /**
         * The comment has been resolved and no changes resulted from the resolution
         */
        RESOLVEDNOCHANGE,
        /**
         * The comment has been resolved and changes are required to address the comment
         */
        RESOLVEDCHANGEREQUIRED,
        /**
         * The comment is acceptable, but resolution of the comment and application of any associated changes have been deferred
         */
        DEFERRED,
        /**
         * The comment is a duplicate of another comment already received
         */
        DUPLICATE,
        /**
         * The comment is resolved and any necessary changes have been applied
         */
        APPLIED,
        /**
         * The necessary changes to the artifact have been published in a new version of the artifact
         */
        PUBLISHED,
        /**
         * added to help the parsers with the generic types
         */
        NULL;

        public static ArtifactAssessment.ArtifactAssessmentWorkflowStatus fromCode(String codeString)
                throws FHIRException {
            if (StringUtils.isBlank(codeString)) {
                return null;
            }
            switch (codeString) {
                case "submitted":
                    return SUBMITTED;
                case "triaged":
                    return TRIAGED;
                case "waiting-for-input":
                    return WAITINGFORINPUT;
                case "resolved-no-change":
                    return RESOLVEDNOCHANGE;
                case "resolved-change-required":
                    return RESOLVEDCHANGEREQUIRED;
                case "deferred":
                    return DEFERRED;
                case "duplicate":
                    return DUPLICATE;
                case "applied":
                    return APPLIED;
                case "published":
                    return PUBLISHED;

                default:
                    if (Configuration.isAcceptInvalidEnums()) {
                        return null;
                    } else {
                        throw new FHIRException("Unknown ArtifactAssessmentWorkflowStatus code '" + codeString + "'");
                    }
            }
        }

        public String toCode() {
            switch (this) {
                case SUBMITTED:
                    return "submitted";
                case TRIAGED:
                    return "triaged";
                case WAITINGFORINPUT:
                    return "waiting-for-input";
                case RESOLVEDNOCHANGE:
                    return "resolved-no-change";
                case RESOLVEDCHANGEREQUIRED:
                    return "resolved-change-required";
                case DEFERRED:
                    return "deferred";
                case DUPLICATE:
                    return "duplicate";
                case APPLIED:
                    return "applied";
                case PUBLISHED:
                    return "published";
                default:
                    return "?";
            }
        }

        public String getSystem() {
            if (this.equals(NULL)) {
                return "?";
            } else {
                return "http://hl7.org/fhir/artifactassessment-workflow-status";
            }
        }

        public String getDefinition() {
            switch (this) {
                case SUBMITTED:
                    return "The comment has been submitted, but the responsible party has not yet been determined, or the responsible party has not yet determined the next steps to be taken.";
                case TRIAGED:
                    return "The comment has been triaged, meaning the responsible party has been determined and next steps have been identified to address the comment.";
                case WAITINGFORINPUT:
                    return "The comment is waiting for input from a specific party before next steps can be taken.";
                case RESOLVEDNOCHANGE:
                    return "The comment has been resolved and no changes resulted from the resolution";
                case RESOLVEDCHANGEREQUIRED:
                    return "The comment has been resolved and changes are required to address the comment";
                case DEFERRED:
                    return "The comment is acceptable, but resolution of the comment and application of any associated changes have been deferred";
                case DUPLICATE:
                    return "The comment is a duplicate of another comment already received";
                case APPLIED:
                    return "The comment is resolved and any necessary changes have been applied";
                case PUBLISHED:
                    return "The necessary changes to the artifact have been published in a new version of the artifact";
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch (this) {
                case SUBMITTED:
                    return "Submitted";
                case TRIAGED:
                    return "Triaged";
                case WAITINGFORINPUT:
                    return "Waiting for Input";
                case RESOLVEDNOCHANGE:
                    return "Resolved - No Change";
                case RESOLVEDCHANGEREQUIRED:
                    return "Resolved - Change Required";
                case DEFERRED:
                    return "Deferred";
                case DUPLICATE:
                    return "Duplicate";
                case APPLIED:
                    return "Applied";
                case PUBLISHED:
                    return "Published";
                default:
                    return "?";
            }
        }
    }

    public static class ArtifactAssessmentWorkflowStatusEnumFactory
            implements EnumFactory<ArtifactAssessment.ArtifactAssessmentWorkflowStatus> {
        public ArtifactAssessment.ArtifactAssessmentWorkflowStatus fromCode(String codeString)
                throws IllegalArgumentException {
            return ArtifactAssessment.ArtifactAssessmentWorkflowStatus.fromCode(codeString);
        }

        public Enumeration<ArtifactAssessment.ArtifactAssessmentWorkflowStatus> fromType(Base code)
                throws FHIRException {
            if (code == null) {
                return null;
            }
            if (code.isEmpty()) {
                return new Enumeration<>(this);
            }
            return new Enumeration<>(
                    this,
                    ArtifactAssessment.ArtifactAssessmentWorkflowStatus.fromCode(
                            ((PrimitiveType<?>) code).asStringValue()));
        }

        public String toCode(ArtifactAssessment.ArtifactAssessmentWorkflowStatus code) {
            return code.toCode();
        }

        public String toSystem(ArtifactAssessment.ArtifactAssessmentWorkflowStatus code) {
            return code.getSystem();
        }
    }

    public enum ArtifactAssessmentDisposition {
        /**
         * The comment is unresolved
         */
        UNRESOLVED,
        /**
         * The comment is not persuasive (rejected in full)
         */
        NOTPERSUASIVE,
        /**
         * The comment is persuasive (accepted in full)
         */
        PERSUASIVE,
        /**
         * The comment is persuasive with modification (partially accepted)
         */
        PERSUASIVEWITHMODIFICATION,
        /**
         * The comment is not persuasive with modification (partially rejected)
         */
        NOTPERSUASIVEWITHMODIFICATION,
        /**
         * added to help the parsers with the generic types
         */
        NULL;

        public static ArtifactAssessment.ArtifactAssessmentDisposition fromCode(String codeString)
                throws FHIRException {
            if (StringUtils.isBlank(codeString)) {
                return null;
            }
            switch (codeString) {
                case "unresolved":
                    return UNRESOLVED;
                case "not-persuasive":
                    return NOTPERSUASIVE;
                case "persuasive":
                    return PERSUASIVE;
                case "persuasive-with-modification":
                    return PERSUASIVEWITHMODIFICATION;
                case "not-persuasive-with-modification":
                    return NOTPERSUASIVEWITHMODIFICATION;

                default:
                    if (Configuration.isAcceptInvalidEnums()) {
                        return null;
                    } else {
                        throw new FHIRException("Unknown ArtifactAssessmentDisposition code '" + codeString + "'");
                    }
            }
        }

        public String toCode() {
            switch (this) {
                case UNRESOLVED:
                    return "unresolved";
                case NOTPERSUASIVE:
                    return "not-persuasive";
                case PERSUASIVE:
                    return "persuasive";
                case PERSUASIVEWITHMODIFICATION:
                    return "persuasive-with-modification";
                case NOTPERSUASIVEWITHMODIFICATION:
                    return "not-persuasive-with-modification";
                default:
                    return "?";
            }
        }

        public String getSystem() {
            if (this.equals(NULL)) {
                return "?";
            } else {
                return "http://hl7.org/fhir/artifactassessment-disposition";
            }
        }

        public String getDefinition() {
            switch (this) {
                case UNRESOLVED:
                    return "The comment is unresolved";
                case NOTPERSUASIVE:
                    return "The comment is not persuasive (rejected in full)";
                case PERSUASIVE:
                    return "The comment is persuasive (accepted in full)";
                case PERSUASIVEWITHMODIFICATION:
                    return "The comment is persuasive with modification (partially accepted)";
                case NOTPERSUASIVEWITHMODIFICATION:
                    return "The comment is not persuasive with modification (partially rejected)";
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch (this) {
                case UNRESOLVED:
                    return "Unresolved";
                case NOTPERSUASIVE:
                    return "Not Persuasive";
                case PERSUASIVE:
                    return "Persuasive";
                case PERSUASIVEWITHMODIFICATION:
                    return "Persuasive with Modification";
                case NOTPERSUASIVEWITHMODIFICATION:
                    return "Not Persuasive with Modification";
                default:
                    return "?";
            }
        }
    }

    public static class ArtifactAssessmentDispositionEnumFactory
            implements EnumFactory<ArtifactAssessment.ArtifactAssessmentDisposition> {
        public ArtifactAssessment.ArtifactAssessmentDisposition fromCode(String codeString)
                throws IllegalArgumentException {
            return ArtifactAssessment.ArtifactAssessmentDisposition.fromCode(codeString);
        }

        public Enumeration<ArtifactAssessment.ArtifactAssessmentDisposition> fromType(Base code) throws FHIRException {
            if (code == null) {
                return null;
            }
            if (code.isEmpty()) {
                return new Enumeration<>(this);
            }
            return new Enumeration<>(
                    this,
                    ArtifactAssessment.ArtifactAssessmentDisposition.fromCode(
                            ((PrimitiveType<?>) code).asStringValue()));
        }

        public String toCode(ArtifactAssessment.ArtifactAssessmentDisposition code) {
            return code.toCode();
        }

        public String toSystem(ArtifactAssessment.ArtifactAssessmentDisposition code) {
            return code.getSystem();
        }
    }

    public static final String ARTIFACT_COMMENT_EXTENSION_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-artifactComment";
    public static final String CONTENT =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentContent";
    public static final String ARTIFACT =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentArtifact";
    public static final String CITEAS = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentCiteAs";
    public static final String TITLE = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentTitle";
    public static final String DATE = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentDate";
    public static final String COPYRIGHT =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentCopyright";
    public static final String APPROVAL_DATE =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentApprovalDate";
    public static final String LAST_REVIEW_DATE =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentLastReviewDate";
    public static final String WORKFLOW_STATUS =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentWorkflowStatus";
    public static final String DISPOSITION =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentDisposition";

    public ArtifactAssessment() {
        super();
    }

    public ArtifactAssessment(Reference artifact) {
        super();
        this.setArtifactExtension(artifact);
    }

    public ArtifactAssessment(CanonicalType artifact) {
        super();
        this.setArtifactExtension(artifact);
    }

    public ArtifactAssessment(UriType artifact) {
        super();
        this.setArtifactExtension(artifact);
    }

    public ArtifactAssessment createArtifactComment(
            CodeType type,
            Reference targetReference,
            Optional<CanonicalType> derivedFromUri,
            Optional<MarkdownType> text,
            Optional<CanonicalType> reference,
            Optional<Reference> user)
            throws FHIRException {
        ArtifactAssessment.ArtifactAssessmentContentExtension content =
                new ArtifactAssessment.ArtifactAssessmentContentExtension();
        if (type != null) {
            content.setInfoType(type);
        }
        if (text.isPresent() && !StringUtils.isBlank(text.get().getValue())) {
            content.setSummary(text.get());
        }
        if (reference.isPresent() && !StringUtils.isBlank(reference.get().getValue())) {
            content.addRelatedArtifact(reference.get(), RelatedArtifactType.CITATION);
        }
        if (user.isPresent() && !StringUtils.isBlank(user.get().getReference())) {
            content.setAuthorExtension(user.get());
        }
        if (derivedFromUri.isPresent()
                && !StringUtils.isBlank(derivedFromUri.get().getValue())) {
            content.addRelatedArtifact(derivedFromUri.get(), RelatedArtifactType.DERIVEDFROM);
        }
        this.addExtension(content);
        setDateExtension(new DateTimeType(new Date()));
        if (targetReference != null && !StringUtils.isBlank(targetReference.getReference())) {
            this.setArtifactExtension(targetReference);
        }
        return this;
    }

    public ArtifactAssessment createArtifactComment(
            CodeType type,
            CanonicalType targetReference,
            Optional<CanonicalType> derivedFromUri,
            Optional<MarkdownType> text,
            Optional<CanonicalType> reference,
            Optional<Reference> user)
            throws FHIRException {
        ArtifactAssessment.ArtifactAssessmentContentExtension content =
                new ArtifactAssessment.ArtifactAssessmentContentExtension();
        if (type != null) {
            content.setInfoType(type);
        }
        if (text.isPresent() && !StringUtils.isBlank(text.get().getValue())) {
            content.setSummary(text.get());
        }
        if (reference.isPresent() && !StringUtils.isBlank(reference.get().getValue())) {
            content.addRelatedArtifact(reference.get(), RelatedArtifactType.CITATION);
        }
        if (user.isPresent() && !StringUtils.isBlank(user.get().getReference())) {
            content.setAuthorExtension(user.get());
        }
        if (derivedFromUri.isPresent()
                && !StringUtils.isBlank(derivedFromUri.get().getValue())) {
            content.addRelatedArtifact(derivedFromUri.get(), RelatedArtifactType.DERIVEDFROM);
        }
        this.addExtension(content);
        setDateExtension(new DateTimeType(new Date()));
        if (targetReference != null && !StringUtils.isBlank(targetReference.getValue())) {
            this.setArtifactExtension(targetReference);
        }
        return this;
    }

    public ArtifactAssessment createArtifactComment(
            CodeType type,
            UriType targetReference,
            Optional<CanonicalType> derivedFromUri,
            Optional<MarkdownType> text,
            Optional<CanonicalType> reference,
            Optional<Reference> user)
            throws FHIRException {
        ArtifactAssessment.ArtifactAssessmentContentExtension content =
                new ArtifactAssessment.ArtifactAssessmentContentExtension();
        if (type != null) {
            content.setInfoType(type);
        }
        if (text.isPresent() && !StringUtils.isBlank(text.get().getValue())) {
            content.setSummary(text.get());
        }
        if (reference.isPresent() && !StringUtils.isBlank(reference.get().getValue())) {
            content.addRelatedArtifact(reference.get(), RelatedArtifactType.CITATION);
        }
        if (user.isPresent() && !StringUtils.isBlank(user.get().getReference())) {
            content.setAuthorExtension(user.get());
        }
        if (derivedFromUri.isPresent()
                && !StringUtils.isBlank(derivedFromUri.get().getValue())) {
            content.addRelatedArtifact(derivedFromUri.get(), RelatedArtifactType.DERIVEDFROM);
        }
        this.addExtension(content);
        setDateExtension(new DateTimeType(new Date()));
        if (targetReference != null && !StringUtils.isBlank(targetReference.getValue())) {
            this.setArtifactExtension(targetReference);
        }
        return this;
    }

    public Optional<RelatedArtifact> getDerivedFromContentRelatedArtifact() {
        Optional<RelatedArtifact> returnedRelatedArtifact = Optional.empty();
        Optional<Extension> content = Optional.ofNullable(this.getExtensionByUrl(CONTENT));
        if (content.isPresent()) {
            Optional<Extension> maybeRelatedArtifact = content.get().getExtension().stream()
                    .filter(extension -> extension
                                    .getUrl()
                                    .equals(ArtifactAssessment.ArtifactAssessmentContentExtension.RELATEDARTIFACT)
                            && ((RelatedArtifact) extension.getValue())
                                    .getType()
                                    .equals(RelatedArtifactType.DERIVEDFROM))
                    .findFirst();
            if (maybeRelatedArtifact.isPresent()) {
                RelatedArtifact derivedFromArtifact =
                        (RelatedArtifact) maybeRelatedArtifact.get().getValue();
                returnedRelatedArtifact = Optional.of(derivedFromArtifact);
            }
        }
        return returnedRelatedArtifact;
    }

    public ArtifactAssessment setDerivedFromContentRelatedArtifact(CanonicalType targetUri) {
        Optional<RelatedArtifact> existingRelatedArtifact = this.getDerivedFromContentRelatedArtifact();
        if (existingRelatedArtifact.isPresent()) {
            RelatedArtifact derivedFromArtifact = existingRelatedArtifact.get();
            derivedFromArtifact.setResourceElement(targetUri);
        } else {
            Extension content = this.getExtensionByUrl(CONTENT);
            if (content == null) {
                content = new Extension(CONTENT);
                this.addExtension(content);
            }
            // this is duplicated from the addRelatedArtifact method
            // since otherwise we get ClassCastExceptions when trying
            // to Cast from Basic to ArtifactAssessment or its Extension subclasses
            RelatedArtifact newRelatedArtifact = new RelatedArtifact();
            newRelatedArtifact.setType(RelatedArtifactType.DERIVEDFROM);
            newRelatedArtifact.setResourceElement(targetUri);
            content.addExtension(
                    ArtifactAssessment.ArtifactAssessmentContentExtension.RELATEDARTIFACT, newRelatedArtifact);
        }
        return this;
    }

    public ArtifactAssessment setDerivedFromContentRelatedArtifact(String targetUri) {
        return setDerivedFromContentRelatedArtifact(new CanonicalType(targetUri));
    }

    public boolean checkArtifactCommentParams(
            String artifactAssessmentType,
            String artifactAssessmentSummary,
            String artifactAssessmentTargetReference,
            String artifactAssessmentRelatedArtifact,
            String derivedFromRelatedArtifactUrl,
            String artifactAssessmentAuthor) {
        var contentCorrect = false;
        var artifactCorrect = false;
        var contentIndex = findIndex(CONTENT, null, this.getExtension());
        if (contentIndex != -1) {
            contentCorrect = checkContent(
                    contentIndex,
                    artifactAssessmentType,
                    artifactAssessmentSummary,
                    artifactAssessmentRelatedArtifact,
                    derivedFromRelatedArtifactUrl,
                    artifactAssessmentAuthor);
        }
        var artifactIndex = findIndex(ARTIFACT, null, this.getExtension());
        if (artifactIndex != -1) {
            var artifactExt = this.getExtension().get(artifactIndex);
            artifactCorrect =
                    ((Reference) artifactExt.getValue()).getReference().equals(artifactAssessmentTargetReference);
        }
        return contentCorrect && artifactCorrect;
    }

    private boolean checkContent(
            int contentIndex,
            String artifactAssessmentType,
            String artifactAssessmentSummary,
            String artifactAssessmentRelatedArtifact,
            String derivedFromRelatedArtifactUrl,
            String artifactAssessmentAuthor) {
        var infoTypeCorrect = false;
        var summaryCorrect = false;
        var citationRelatedArtifactCorrect = false;
        var derivedFromRelatedArtifactCorrect = false;
        var authorCorrect = false;
        var contentExt = this.getExtension().get(contentIndex);
        int infoTypeIndex = findIndex(
                ArtifactAssessment.ArtifactAssessmentContentExtension.INFOTYPE, null, contentExt.getExtension());
        if (infoTypeIndex != -1) {
            var infoTypeExt = contentExt.getExtension().get(infoTypeIndex);
            infoTypeCorrect = ((CodeType) infoTypeExt.getValue()).getCode().equals(artifactAssessmentType);
        }
        var summaryIndex = findIndex(
                ArtifactAssessment.ArtifactAssessmentContentExtension.SUMMARY, null, contentExt.getExtension());
        if (summaryIndex != -1) {
            var summaryExt = contentExt.getExtension().get(summaryIndex);
            summaryCorrect = ((StringType) summaryExt.getValue()).getValue().equals(artifactAssessmentSummary);
        }
        var relatedArtifactList = contentExt.getExtension().stream()
                .filter(e -> e.getUrl().equals(ArtifactAssessment.ArtifactAssessmentContentExtension.RELATEDARTIFACT))
                .collect(Collectors.toList());
        if (!relatedArtifactList.isEmpty()) {
            var maybeCitation = relatedArtifactList.stream()
                    .filter(ext -> ((RelatedArtifact) ext.getValue()).getType().equals(RelatedArtifactType.CITATION))
                    .findAny();
            var maybeDerivedFrom = relatedArtifactList.stream()
                    .filter(ext -> ((RelatedArtifact) ext.getValue()).getType().equals(RelatedArtifactType.DERIVEDFROM))
                    .findAny();
            if (maybeCitation.isPresent()) {
                var citation = maybeCitation.get();
                citationRelatedArtifactCorrect =
                        ((RelatedArtifact) citation.getValue()).getResource().equals(artifactAssessmentRelatedArtifact);
            }
            if (maybeDerivedFrom.isPresent()) {
                var derivedFrom = maybeDerivedFrom.get();
                derivedFromRelatedArtifactCorrect =
                        ((RelatedArtifact) derivedFrom.getValue()).getResource().equals(derivedFromRelatedArtifactUrl);
            }
        }
        var authorIndex = findIndex(
                ArtifactAssessment.ArtifactAssessmentContentExtension.AUTHOR, null, contentExt.getExtension());
        if (authorIndex != -1) {
            var authorExt = contentExt.getExtension().get(authorIndex);
            authorCorrect = ((Reference) authorExt.getValue()).getReference().equals(artifactAssessmentAuthor);
        }

        return infoTypeCorrect
                && summaryCorrect
                && citationRelatedArtifactCorrect
                && derivedFromRelatedArtifactCorrect
                && authorCorrect;
    }

    public boolean isValidArtifactComment() {
        var infoTypeExists = false;
        var summaryExists = false;
        var relatedArtifactExists = false;
        var authorExists = false;
        var dateExists = findIndex(DATE, null, this.getExtension()) != -1;
        var artifactExists = findIndex(ARTIFACT, null, this.getExtension()) != -1;
        var contentIndex = findIndex(CONTENT, null, this.getExtension());
        if (contentIndex != -1) {
            var content = this.getExtension().get(contentIndex);
            infoTypeExists = findIndex(
                            ArtifactAssessment.ArtifactAssessmentContentExtension.INFOTYPE,
                            null,
                            content.getExtension())
                    != -1;
            summaryExists = findIndex(
                            ArtifactAssessment.ArtifactAssessmentContentExtension.SUMMARY, null, content.getExtension())
                    != -1;
            relatedArtifactExists = findIndex(
                            ArtifactAssessment.ArtifactAssessmentContentExtension.RELATEDARTIFACT,
                            null,
                            content.getExtension())
                    != -1;
            authorExists = findIndex(
                            ArtifactAssessment.ArtifactAssessmentContentExtension.AUTHOR, null, content.getExtension())
                    != -1;
        }
        return (infoTypeExists || summaryExists || relatedArtifactExists || authorExists)
                && dateExists
                && artifactExists;
    }

    List<ArtifactAssessment.ArtifactAssessmentContentExtension> getContent() {
        return this.getExtension().stream()
                .filter(ext -> ext.getUrl().equals(CONTENT))
                .map(ArtifactAssessment.ArtifactAssessmentContentExtension.class::cast)
                .collect(Collectors.toList());
    }

    public ArtifactAssessment setArtifactExtension(CanonicalType target) {
        if (target != null && target.getValue() != null) {
            int index = findIndex(ARTIFACT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentArtifactExtension(target));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentArtifactExtension(target));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactExtension(Reference target) {
        if (target != null && target.getReference() != null) {
            int index = findIndex(ARTIFACT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentArtifactExtension(target));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentArtifactExtension(target));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactExtension(UriType target) {
        if (target != null && target.getValue() != null) {
            int index = findIndex(ARTIFACT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentArtifactExtension(target));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentArtifactExtension(target));
            }
        }
        return this;
    }

    public ArtifactAssessment setDateExtension(DateTimeType date) {
        if (date != null) {
            int index = findIndex(DATE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentDateExtension(date));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentDateExtension(date));
            }
        }
        return this;
    }

    public ArtifactAssessment setLastReviewDateExtension(DateType reviewDate) {
        if (reviewDate != null) {
            int index = findIndex(LAST_REVIEW_DATE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentLastReviewDateExtension(reviewDate));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentLastReviewDateExtension(reviewDate));
            }
        }
        return this;
    }

    public ArtifactAssessment setApprovalDateExtension(DateType approvalDate) {
        if (approvalDate != null) {
            int index = findIndex(APPROVAL_DATE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentApprovalDateExtension(approvalDate));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentApprovalDateExtension(approvalDate));
            }
        }
        return this;
    }

    public ArtifactAssessment setTitleExtension(MarkdownType title) {
        if (title != null) {
            int index = findIndex(TITLE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentTitleExtension(title));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentTitleExtension(title));
            }
        }
        return this;
    }

    public ArtifactAssessment setCopyrightExtension(MarkdownType copyright) {
        if (copyright != null) {
            int index = findIndex(COPYRIGHT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentCopyrightExtension(copyright));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentCopyrightExtension(copyright));
            }
        }
        return this;
    }

    public ArtifactAssessment setCiteAsExtension(MarkdownType citeAs) {
        if (citeAs != null) {
            int index = findIndex(CITEAS, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentCiteAsExtension(citeAs));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentCiteAsExtension(citeAs));
            }
        }
        return this;
    }

    public ArtifactAssessment setCiteAsExtension(Reference citeAs) {
        if (citeAs != null) {
            int index = findIndex(CITEAS, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentCiteAsExtension(citeAs));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentCiteAsExtension(citeAs));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactAssessmentWorkflowStatusExtension(
            Enumeration<ArtifactAssessment.ArtifactAssessmentWorkflowStatus> status) {
        if (status != null) {
            int index = findIndex(WORKFLOW_STATUS, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentWorkflowStatusExtension(status));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentWorkflowStatusExtension(status));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactAssessmentDispositionExtension(
            Enumeration<ArtifactAssessment.ArtifactAssessmentDisposition> status) {
        if (status != null) {
            int index = findIndex(DISPOSITION, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessment.ArtifactAssessmentDispositionExtension(status));
            } else {
                this.addExtension(new ArtifactAssessment.ArtifactAssessmentDispositionExtension(status));
            }
        }
        return this;
    }

    private int findIndex(String url, DataType value, List<Extension> extensions) {
        Optional<Extension> existingExtension;
        if (value != null) {
            existingExtension = extensions.stream()
                    .filter(e -> e.getUrl().equals(url) && e.getValue().equals(value))
                    .findAny();
        } else {
            existingExtension =
                    extensions.stream().filter(e -> e.getUrl().equals(url)).findAny();
        }
        if (existingExtension.isPresent()) {
            return extensions.indexOf(existingExtension.get());
        } else {
            return -1;
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentContentExtension", isSpecialization = true, profileOf = Extension.class)
    public class ArtifactAssessmentContentExtension extends Extension {
        public static final String INFOTYPE = "informationType";
        public static final String SUMMARY = "summary";
        public static final String TYPE = "type";
        public static final String CLASSIFIER = "classifier";
        public static final String QUANTITY = "quantity";
        public static final String AUTHOR = "author";
        public static final String PATH = "path";
        public static final String RELATEDARTIFACT = "relatedArtifact";
        public static final String FREETOSHARE = "freeToShare";
        public static final String COMPONENT = "component";

        public ArtifactAssessmentContentExtension() throws FHIRException {
            super(CONTENT);
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension setInfoType(CodeType infoType) throws FHIRException {
            if (infoType != null) {
                int index = findIndex(INFOTYPE, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(
                            index,
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentInformationTypeExtension(infoType));
                } else {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentInformationTypeExtension(infoType));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension setSummary(MarkdownType summary) {
            if (summary != null) {
                int index = findIndex(SUMMARY, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(
                            index,
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentSummaryExtension(summary));
                } else {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentSummaryExtension(summary));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension addRelatedArtifact(
                CanonicalType reference, RelatedArtifactType type) {
            if (reference != null) {
                RelatedArtifact newRelatedArtifact = new RelatedArtifact();
                newRelatedArtifact.setType(type);
                newRelatedArtifact.setResourceElement(reference);
                int index = findIndex(RELATEDARTIFACT, newRelatedArtifact, this.getExtension());
                if (index == -1) {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentRelatedArtifactExtension(newRelatedArtifact));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension addComponent(
                ArtifactAssessment.ArtifactAssessmentContentExtension component) {
            if (component != null) {
                this.addExtension(
                        new ArtifactAssessment.ArtifactAssessmentContentExtension
                                .ArtifactAssessmentContentComponentExtension(component));
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension setAuthorExtension(Reference author) {
            if (author != null) {
                int index = findIndex(AUTHOR, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(
                            index,
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentAuthorExtension(author));
                } else {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentAuthorExtension(author));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension setQuantityExtension(Quantity quantity) {
            if (quantity != null) {
                int index = findIndex(QUANTITY, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(
                            index,
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentQuantityExtension(quantity));
                } else {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentQuantityExtension(quantity));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension setTypeExtension(CodeableConcept type) {
            if (type != null) {
                int index = findIndex(TYPE, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(
                            index,
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentTypeExtension(type));
                } else {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentTypeExtension(type));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension setFreeToShareExtension(BooleanType freeToShare) {
            if (freeToShare != null) {
                int index = findIndex(FREETOSHARE, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(
                            index,
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentFreeToShareExtension(freeToShare));
                } else {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentFreeToShareExtension(freeToShare));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension addClassifierExtension(CodeableConcept classifier) {
            if (classifier != null) {
                int index = findIndex(CLASSIFIER, classifier, this.getExtension());
                if (index == -1) {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentClassifierExtension(classifier));
                }
            }
            return this;
        }

        ArtifactAssessment.ArtifactAssessmentContentExtension addPathExtension(UriType path) {
            if (path != null) {
                int index = findIndex(PATH, path, this.getExtension());
                if (index == -1) {
                    this.addExtension(
                            new ArtifactAssessment.ArtifactAssessmentContentExtension
                                    .ArtifactAssessmentContentPathExtension(path));
                }
            }
            return this;
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentInformationTypeExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentInformationTypeExtension extends Extension {
            public ArtifactAssessmentContentInformationTypeExtension(CodeType informationTypeCode) {
                super(INFOTYPE);
                // validate code
                ArtifactAssessment.ArtifactAssessmentContentInformationType.fromCode(informationTypeCode.getValue());
                this.setValue(informationTypeCode);
            }
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentSummaryExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentSummaryExtension extends Extension {
            public ArtifactAssessmentContentSummaryExtension(MarkdownType summary) {
                super(SUMMARY, summary);
            }
        }

        @SuppressWarnings("squid:S2387")
        @DatatypeDef(
                name = "ArtifactAssessmentContentTypeExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentTypeExtension extends Extension {
            @ca.uhn.fhir.model.api.annotation.Binding(valueSet = "http://hl7.org/fhir/ValueSet/certainty-type")
            protected CodeableConcept value;

            public ArtifactAssessmentContentTypeExtension(CodeableConcept typeConcept) {
                super(TYPE, typeConcept);
            }
        }

        @SuppressWarnings("squid:S2387")
        @DatatypeDef(
                name = "ArtifactAssessmentContentClassifierExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentClassifierExtension extends Extension {
            @ca.uhn.fhir.model.api.annotation.Binding(valueSet = "http://hl7.org/fhir/ValueSet/certainty-rating")
            protected CodeableConcept value;

            public ArtifactAssessmentContentClassifierExtension(CodeableConcept classifierConcept) {
                super(CLASSIFIER, classifierConcept);
            }
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentQuantityExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentQuantityExtension extends Extension {
            public ArtifactAssessmentContentQuantityExtension(Quantity quantity) {
                super(QUANTITY, quantity);
            }
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentAuthorExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentAuthorExtension extends Extension {
            public ArtifactAssessmentContentAuthorExtension(Reference author) {
                super(AUTHOR, author);
            }
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentPathExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentPathExtension extends Extension {
            public ArtifactAssessmentContentPathExtension(UriType path) {
                super(PATH, path);
            }
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentRelatedArtifactExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentRelatedArtifactExtension extends Extension {
            public ArtifactAssessmentContentRelatedArtifactExtension(RelatedArtifact relatedArtifact) {
                super(RELATEDARTIFACT, relatedArtifact);
            }
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentFreeToShareExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentFreeToShareExtension extends Extension {
            public ArtifactAssessmentContentFreeToShareExtension(BooleanType freeToShare) {
                super(FREETOSHARE, freeToShare);
            }
        }

        @DatatypeDef(
                name = "ArtifactAssessmentContentComponentExtension",
                isSpecialization = true,
                profileOf = Extension.class)
        private class ArtifactAssessmentContentComponentExtension extends Extension {
            public ArtifactAssessmentContentComponentExtension(
                    ArtifactAssessment.ArtifactAssessmentContentExtension contentExtension) {
                super(COMPONENT, contentExtension);
            }
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentArtifactExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentArtifactExtension extends Extension {
        public ArtifactAssessmentArtifactExtension(CanonicalType target) {
            super(ARTIFACT, target);
        }

        public ArtifactAssessmentArtifactExtension(Reference target) {
            super(ARTIFACT, target);
        }

        public ArtifactAssessmentArtifactExtension(UriType target) {
            super(ARTIFACT, target);
        }
    }

    @DatatypeDef(
            name = "ArtifactAssessmentWorkflowStatusExtension",
            isSpecialization = true,
            profileOf = Extension.class)
    private class ArtifactAssessmentWorkflowStatusExtension extends Extension {
        public ArtifactAssessmentWorkflowStatusExtension(
                Enumeration<ArtifactAssessment.ArtifactAssessmentWorkflowStatus> status) {
            super(WORKFLOW_STATUS, status);
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentDispositionExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentDispositionExtension extends Extension {
        public ArtifactAssessmentDispositionExtension(
                Enumeration<ArtifactAssessment.ArtifactAssessmentDisposition> disposition) {
            super(DISPOSITION, disposition);
        }
    }

    @DatatypeDef(
            name = "ArtifactAssessmentLastReviewDateExtension",
            isSpecialization = true,
            profileOf = Extension.class)
    private class ArtifactAssessmentLastReviewDateExtension extends Extension {
        public ArtifactAssessmentLastReviewDateExtension(DateType lastReviewDate) {
            super(LAST_REVIEW_DATE, lastReviewDate);
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentApprovalDateExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentApprovalDateExtension extends Extension {
        public ArtifactAssessmentApprovalDateExtension(DateType approvalDate) {
            super(APPROVAL_DATE, approvalDate);
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentCopyrightExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentCopyrightExtension extends Extension {
        public ArtifactAssessmentCopyrightExtension(MarkdownType copyright) {
            super(COPYRIGHT, copyright);
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentDateExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentDateExtension extends Extension {
        public ArtifactAssessmentDateExtension(DateTimeType date) {
            super(DATE, date);
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentTitleExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentTitleExtension extends Extension {
        public ArtifactAssessmentTitleExtension(StringType title) {
            super(TITLE, title);
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentCiteAsExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentCiteAsExtension extends Extension {
        public ArtifactAssessmentCiteAsExtension(Reference citation) {
            super(CITEAS, citation);
        }

        public ArtifactAssessmentCiteAsExtension(MarkdownType citation) {
            super(CITEAS, citation);
        }
    }
}
