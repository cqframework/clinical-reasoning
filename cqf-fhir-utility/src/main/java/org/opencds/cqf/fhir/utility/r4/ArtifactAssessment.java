package org.opencds.cqf.fhir.utility.r4;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Configuration;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.EnumFactory;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;

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

        public static ArtifactAssessmentContentInformationType fromCode(String codeString) throws FHIRException {
            if (codeString == null || "".equals(codeString)) return null;
            if ("comment".equals(codeString)) return COMMENT;
            if ("classifier".equals(codeString)) return CLASSIFIER;
            if ("rating".equals(codeString)) return RATING;
            if ("container".equals(codeString)) return CONTAINER;
            if ("response".equals(codeString)) return RESPONSE;
            if ("change-request".equals(codeString)) return CHANGEREQUEST;
            if (Configuration.isAcceptInvalidEnums()) return null;
            else throw new FHIRException("Unknown ArtifactAssessment '" + codeString + "'");
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
                case COMMENT:
                    return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
                case CLASSIFIER:
                    return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
                case RATING:
                    return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
                case CONTAINER:
                    return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
                case RESPONSE:
                    return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type";
                case CHANGEREQUEST:
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
            implements EnumFactory<ArtifactAssessmentContentInformationType> {
        public ArtifactAssessmentContentInformationType fromCode(String codeString) throws IllegalArgumentException {
            if (codeString == null || "".equals(codeString))
                if (codeString == null || "".equals(codeString)) return null;
            if ("comment".equals(codeString)) return ArtifactAssessmentContentInformationType.COMMENT;
            if ("classifier".equals(codeString)) return ArtifactAssessmentContentInformationType.CLASSIFIER;
            if ("rating".equals(codeString)) return ArtifactAssessmentContentInformationType.RATING;
            if ("container".equals(codeString)) return ArtifactAssessmentContentInformationType.CONTAINER;
            if ("response".equals(codeString)) return ArtifactAssessmentContentInformationType.RESPONSE;
            if ("change-request".equals(codeString)) return ArtifactAssessmentContentInformationType.CHANGEREQUEST;
            throw new IllegalArgumentException("Unknown ArtifactCommentType code '" + codeString + "'");
        }

        public Enumeration<ArtifactAssessmentContentInformationType> fromType(Base code) throws FHIRException {
            if (code == null) return null;
            if (code.isEmpty()) return new Enumeration<ArtifactAssessmentContentInformationType>(this);
            String codeString = ((PrimitiveType<?>) code).asStringValue();
            if (codeString == null || "".equals(codeString)) return null;
            if ("comment".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentInformationType>(
                        this, ArtifactAssessmentContentInformationType.COMMENT);
            if ("classifier".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentInformationType>(
                        this, ArtifactAssessmentContentInformationType.CLASSIFIER);
            if ("rating".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentInformationType>(
                        this, ArtifactAssessmentContentInformationType.RATING);
            if ("container".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentInformationType>(
                        this, ArtifactAssessmentContentInformationType.CONTAINER);
            if ("response".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentInformationType>(
                        this, ArtifactAssessmentContentInformationType.RESPONSE);
            if ("change-request".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentInformationType>(
                        this, ArtifactAssessmentContentInformationType.CHANGEREQUEST);

            throw new FHIRException("Unknown ArtifactCommentType code '" + codeString + "'");
        }

        public String toCode(ArtifactAssessmentContentInformationType code) {
            if (code == ArtifactAssessmentContentInformationType.COMMENT) return "comment";
            if (code == ArtifactAssessmentContentInformationType.CLASSIFIER) return "classifier";
            if (code == ArtifactAssessmentContentInformationType.RATING) return "rating";
            if (code == ArtifactAssessmentContentInformationType.CONTAINER) return "container";
            if (code == ArtifactAssessmentContentInformationType.RESPONSE) return "response";
            if (code == ArtifactAssessmentContentInformationType.CHANGEREQUEST) return "change-request";
            return "?";
        }

        public String toSystem(ArtifactAssessmentContentInformationType code) {
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

        public static ArtifactAssessmentContentClassifier fromCode(String codeString) throws FHIRException {
            if (codeString == null || "".equals(codeString)) return null;
            if ("high".equals(codeString)) return HIGH;
            if ("moderate".equals(codeString)) return MODERATE;
            if ("low".equals(codeString)) return LOW;
            if ("very-low".equals(codeString)) return VERY_LOW;
            if ("no-concern".equals(codeString)) return NO_CONCERN;
            if ("serious-concern".equals(codeString)) return SERIOUS_CONCERN;
            if ("very-serious-concern".equals(codeString)) return VERY_SERIOUS_CONCERN;
            if ("extremely-serious-concern".equals(codeString)) return EXTREMELY_SERIOUS_CONCERN;
            if ("present".equals(codeString)) return PRESENT;
            if ("absent".equals(codeString)) return ABSENT;
            if ("no-change".equals(codeString)) return NO_CHANGE;
            if ("downcode1".equals(codeString)) return DOWNCODE1;
            if ("downcode2".equals(codeString)) return DOWNCODE2;
            if ("downcode3".equals(codeString)) return DOWNCODE3;
            if ("upcode1".equals(codeString)) return UPCODE1;
            if ("upcode2".equals(codeString)) return UPCODE2;
            if (Configuration.isAcceptInvalidEnums()) return null;
            else throw new FHIRException("Unknown ArtifactAssessment '" + codeString + "'");
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
                case HIGH:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case MODERATE:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case LOW:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case VERY_LOW:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case NO_CONCERN:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case SERIOUS_CONCERN:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case VERY_SERIOUS_CONCERN:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case EXTREMELY_SERIOUS_CONCERN:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case PRESENT:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case ABSENT:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case NO_CHANGE:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case DOWNCODE1:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case DOWNCODE2:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case DOWNCODE3:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case UPCODE1:
                    return "http://terminology.hl7.org/CodeSystem/certainty-rating";
                case UPCODE2:
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
            implements EnumFactory<ArtifactAssessmentContentClassifier> {
        public ArtifactAssessmentContentClassifier fromCode(String codeString) throws IllegalArgumentException {
            if (codeString == null || "".equals(codeString))
                if (codeString == null || "".equals(codeString)) return null;
            if ("high".equals(codeString)) return ArtifactAssessmentContentClassifier.HIGH;
            if ("moderate".equals(codeString)) return ArtifactAssessmentContentClassifier.MODERATE;
            if ("low".equals(codeString)) return ArtifactAssessmentContentClassifier.LOW;
            if ("very-low".equals(codeString)) return ArtifactAssessmentContentClassifier.VERY_LOW;
            if ("no-concern".equals(codeString)) return ArtifactAssessmentContentClassifier.NO_CONCERN;
            if ("serious-concern".equals(codeString)) return ArtifactAssessmentContentClassifier.SERIOUS_CONCERN;
            if ("very-serious-concern".equals(codeString))
                return ArtifactAssessmentContentClassifier.VERY_SERIOUS_CONCERN;
            if ("extremely-serious-concern".equals(codeString))
                return ArtifactAssessmentContentClassifier.EXTREMELY_SERIOUS_CONCERN;
            if ("present".equals(codeString)) return ArtifactAssessmentContentClassifier.PRESENT;
            if ("absent".equals(codeString)) return ArtifactAssessmentContentClassifier.ABSENT;
            if ("no-change".equals(codeString)) return ArtifactAssessmentContentClassifier.NO_CHANGE;
            if ("downcode1".equals(codeString)) return ArtifactAssessmentContentClassifier.DOWNCODE1;
            if ("downcode2".equals(codeString)) return ArtifactAssessmentContentClassifier.DOWNCODE2;
            if ("downcode3".equals(codeString)) return ArtifactAssessmentContentClassifier.DOWNCODE3;
            if ("upcode1".equals(codeString)) return ArtifactAssessmentContentClassifier.UPCODE1;
            if ("upcode2".equals(codeString)) return ArtifactAssessmentContentClassifier.UPCODE2;
            throw new IllegalArgumentException("Unknown ArtifactCommentType code '" + codeString + "'");
        }

        public Enumeration<ArtifactAssessmentContentClassifier> fromType(Base code) throws FHIRException {
            if (code == null) return null;
            if (code.isEmpty()) return new Enumeration<ArtifactAssessmentContentClassifier>(this);
            String codeString = ((PrimitiveType<?>) code).asStringValue();
            if (codeString == null || "".equals(codeString)) return null;
            if ("high".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.HIGH);
            if ("moderate".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.MODERATE);
            if ("low".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.LOW);
            if ("very-low".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.VERY_LOW);
            if ("no-concern".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.NO_CONCERN);
            if ("serious-concern".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.SERIOUS_CONCERN);
            if ("very-serious-concern".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.VERY_SERIOUS_CONCERN);
            if ("extremely-serious-concern".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.EXTREMELY_SERIOUS_CONCERN);
            if ("present".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.PRESENT);
            if ("absent".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.ABSENT);
            if ("no-change".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.NO_CHANGE);
            if ("downcode1".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.DOWNCODE1);
            if ("downcode2".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.DOWNCODE2);
            if ("downcode3".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.DOWNCODE3);
            if ("upcode1".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.UPCODE1);
            if ("upcode2".equals(codeString))
                return new Enumeration<ArtifactAssessmentContentClassifier>(
                        this, ArtifactAssessmentContentClassifier.UPCODE2);
            throw new FHIRException("Unknown ArtifactCommentType code '" + codeString + "'");
        }

        public String toCode(ArtifactAssessmentContentClassifier code) {
            if (code == ArtifactAssessmentContentClassifier.HIGH) return "high";
            if (code == ArtifactAssessmentContentClassifier.MODERATE) return "moderate";
            if (code == ArtifactAssessmentContentClassifier.LOW) return "low";
            if (code == ArtifactAssessmentContentClassifier.VERY_LOW) return "very-low";
            if (code == ArtifactAssessmentContentClassifier.NO_CONCERN) return "no-concern";
            if (code == ArtifactAssessmentContentClassifier.SERIOUS_CONCERN) return "serious-concern";
            if (code == ArtifactAssessmentContentClassifier.VERY_SERIOUS_CONCERN) return "very-serious-concern";
            if (code == ArtifactAssessmentContentClassifier.EXTREMELY_SERIOUS_CONCERN)
                return "extremely-serious-concern";
            if (code == ArtifactAssessmentContentClassifier.PRESENT) return "present";
            if (code == ArtifactAssessmentContentClassifier.ABSENT) return "absent";
            if (code == ArtifactAssessmentContentClassifier.NO_CHANGE) return "no-change";
            if (code == ArtifactAssessmentContentClassifier.DOWNCODE1) return "downcode1";
            if (code == ArtifactAssessmentContentClassifier.DOWNCODE2) return "downcode2";
            if (code == ArtifactAssessmentContentClassifier.DOWNCODE3) return "downcode3";
            if (code == ArtifactAssessmentContentClassifier.UPCODE1) return "upcode1";
            if (code == ArtifactAssessmentContentClassifier.UPCODE2) return "upcode2";
            return "?";
        }

        public String toSystem(ArtifactAssessmentContentClassifier code) {
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

        public static ArtifactAssessmentWorkflowStatus fromCode(String codeString) throws FHIRException {
            if (codeString == null || "".equals(codeString)) return null;
            if ("submitted".equals(codeString)) return SUBMITTED;
            if ("triaged".equals(codeString)) return TRIAGED;
            if ("waiting-for-input".equals(codeString)) return WAITINGFORINPUT;
            if ("resolved-no-change".equals(codeString)) return RESOLVEDNOCHANGE;
            if ("resolved-change-required".equals(codeString)) return RESOLVEDCHANGEREQUIRED;
            if ("deferred".equals(codeString)) return DEFERRED;
            if ("duplicate".equals(codeString)) return DUPLICATE;
            if ("applied".equals(codeString)) return APPLIED;
            if ("published".equals(codeString)) return PUBLISHED;
            if (Configuration.isAcceptInvalidEnums()) return null;
            else throw new FHIRException("Unknown ArtifactAssessmentWorkflowStatus code '" + codeString + "'");
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
            switch (this) {
                case SUBMITTED:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case TRIAGED:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case WAITINGFORINPUT:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case RESOLVEDNOCHANGE:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case RESOLVEDCHANGEREQUIRED:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case DEFERRED:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case DUPLICATE:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case APPLIED:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                case PUBLISHED:
                    return "http://hl7.org/fhir/artifactassessment-workflow-status";
                default:
                    return "?";
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
            implements EnumFactory<ArtifactAssessmentWorkflowStatus> {
        public ArtifactAssessmentWorkflowStatus fromCode(String codeString) throws IllegalArgumentException {
            if (codeString == null || "".equals(codeString))
                if (codeString == null || "".equals(codeString)) return null;
            if ("submitted".equals(codeString)) return ArtifactAssessmentWorkflowStatus.SUBMITTED;
            if ("triaged".equals(codeString)) return ArtifactAssessmentWorkflowStatus.TRIAGED;
            if ("waiting-for-input".equals(codeString)) return ArtifactAssessmentWorkflowStatus.WAITINGFORINPUT;
            if ("resolved-no-change".equals(codeString)) return ArtifactAssessmentWorkflowStatus.RESOLVEDNOCHANGE;
            if ("resolved-change-required".equals(codeString))
                return ArtifactAssessmentWorkflowStatus.RESOLVEDCHANGEREQUIRED;
            if ("deferred".equals(codeString)) return ArtifactAssessmentWorkflowStatus.DEFERRED;
            if ("duplicate".equals(codeString)) return ArtifactAssessmentWorkflowStatus.DUPLICATE;
            if ("applied".equals(codeString)) return ArtifactAssessmentWorkflowStatus.APPLIED;
            if ("published".equals(codeString)) return ArtifactAssessmentWorkflowStatus.PUBLISHED;
            else
                throw new IllegalArgumentException(
                        "Unknown ArtifactAssessmentWorkflowStatus code '" + codeString + "'");
        }

        public Enumeration<ArtifactAssessmentWorkflowStatus> fromType(Base code) throws FHIRException {
            if (code == null) return null;
            if (code.isEmpty()) return new Enumeration<ArtifactAssessmentWorkflowStatus>(this);
            String codeString = ((PrimitiveType<?>) code).asStringValue();
            if (codeString == null || "".equals(codeString)) return null;
            if ("submitted".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.SUBMITTED);
            if ("triaged".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.TRIAGED);
            if ("waiting-for-input".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.WAITINGFORINPUT);
            if ("resolved-no-change".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.RESOLVEDNOCHANGE);
            if ("resolved-change-required".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.RESOLVEDCHANGEREQUIRED);
            if ("deferred".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.DEFERRED);
            if ("duplicate".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.DUPLICATE);
            if ("applied".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.APPLIED);
            if ("published".equals(codeString))
                return new Enumeration<ArtifactAssessmentWorkflowStatus>(
                        this, ArtifactAssessmentWorkflowStatus.PUBLISHED);
            throw new FHIRException("Unknown ArtifactAssessmentWorkflowStatus code '" + codeString + "'");
        }

        public String toCode(ArtifactAssessmentWorkflowStatus code) {
            if (code == ArtifactAssessmentWorkflowStatus.SUBMITTED) return "submitted";
            if (code == ArtifactAssessmentWorkflowStatus.TRIAGED) return "triaged";
            if (code == ArtifactAssessmentWorkflowStatus.WAITINGFORINPUT) return "waiting-for-input";
            if (code == ArtifactAssessmentWorkflowStatus.RESOLVEDNOCHANGE) return "resolved-no-change";
            if (code == ArtifactAssessmentWorkflowStatus.RESOLVEDCHANGEREQUIRED) return "resolved-change-required";
            if (code == ArtifactAssessmentWorkflowStatus.DEFERRED) return "deferred";
            if (code == ArtifactAssessmentWorkflowStatus.DUPLICATE) return "duplicate";
            if (code == ArtifactAssessmentWorkflowStatus.APPLIED) return "applied";
            if (code == ArtifactAssessmentWorkflowStatus.PUBLISHED) return "published";
            return "?";
        }

        public String toSystem(ArtifactAssessmentWorkflowStatus code) {
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

        public static ArtifactAssessmentDisposition fromCode(String codeString) throws FHIRException {
            if (codeString == null || "".equals(codeString)) return null;
            if ("unresolved".equals(codeString)) return UNRESOLVED;
            if ("not-persuasive".equals(codeString)) return NOTPERSUASIVE;
            if ("persuasive".equals(codeString)) return PERSUASIVE;
            if ("persuasive-with-modification".equals(codeString)) return PERSUASIVEWITHMODIFICATION;
            if ("not-persuasive-with-modification".equals(codeString)) return NOTPERSUASIVEWITHMODIFICATION;
            if (Configuration.isAcceptInvalidEnums()) return null;
            else throw new FHIRException("Unknown ArtifactAssessmentDisposition code '" + codeString + "'");
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
            switch (this) {
                case UNRESOLVED:
                    return "http://hl7.org/fhir/artifactassessment-disposition";
                case NOTPERSUASIVE:
                    return "http://hl7.org/fhir/artifactassessment-disposition";
                case PERSUASIVE:
                    return "http://hl7.org/fhir/artifactassessment-disposition";
                case PERSUASIVEWITHMODIFICATION:
                    return "http://hl7.org/fhir/artifactassessment-disposition";
                case NOTPERSUASIVEWITHMODIFICATION:
                    return "http://hl7.org/fhir/artifactassessment-disposition";
                default:
                    return "?";
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

    public static class ArtifactAssessmentDispositionEnumFactory implements EnumFactory<ArtifactAssessmentDisposition> {
        public ArtifactAssessmentDisposition fromCode(String codeString) throws IllegalArgumentException {
            if (codeString == null || "".equals(codeString))
                if (codeString == null || "".equals(codeString)) return null;
            if ("unresolved".equals(codeString)) return ArtifactAssessmentDisposition.UNRESOLVED;
            if ("not-persuasive".equals(codeString)) return ArtifactAssessmentDisposition.NOTPERSUASIVE;
            if ("persuasive".equals(codeString)) return ArtifactAssessmentDisposition.PERSUASIVE;
            if ("persuasive-with-modification".equals(codeString))
                return ArtifactAssessmentDisposition.PERSUASIVEWITHMODIFICATION;
            if ("not-persuasive-with-modification".equals(codeString))
                return ArtifactAssessmentDisposition.NOTPERSUASIVEWITHMODIFICATION;
            throw new IllegalArgumentException("Unknown ArtifactAssessmentDisposition code '" + codeString + "'");
        }

        public Enumeration<ArtifactAssessmentDisposition> fromType(Base code) throws FHIRException {
            if (code == null) return null;
            if (code.isEmpty()) return new Enumeration<ArtifactAssessmentDisposition>(this);
            String codeString = ((PrimitiveType<?>) code).asStringValue();
            if (codeString == null || "".equals(codeString)) return null;
            if ("unresolved".equals(codeString))
                return new Enumeration<ArtifactAssessmentDisposition>(this, ArtifactAssessmentDisposition.UNRESOLVED);
            if ("not-persuasive".equals(codeString))
                return new Enumeration<ArtifactAssessmentDisposition>(
                        this, ArtifactAssessmentDisposition.NOTPERSUASIVE);
            if ("persuasive".equals(codeString))
                return new Enumeration<ArtifactAssessmentDisposition>(this, ArtifactAssessmentDisposition.PERSUASIVE);
            if ("persuasive-with-modification".equals(codeString))
                return new Enumeration<ArtifactAssessmentDisposition>(
                        this, ArtifactAssessmentDisposition.PERSUASIVEWITHMODIFICATION);
            if ("not-persuasive-with-modification".equals(codeString))
                return new Enumeration<ArtifactAssessmentDisposition>(
                        this, ArtifactAssessmentDisposition.NOTPERSUASIVEWITHMODIFICATION);
            throw new FHIRException("Unknown ArtifactAssessmentDisposition code '" + codeString + "'");
        }

        public String toCode(ArtifactAssessmentDisposition code) {
            if (code == ArtifactAssessmentDisposition.UNRESOLVED) return "unresolved";
            if (code == ArtifactAssessmentDisposition.NOTPERSUASIVE) return "not-persuasive";
            if (code == ArtifactAssessmentDisposition.PERSUASIVE) return "persuasive";
            if (code == ArtifactAssessmentDisposition.PERSUASIVEWITHMODIFICATION) return "persuasive-with-modification";
            if (code == ArtifactAssessmentDisposition.NOTPERSUASIVEWITHMODIFICATION)
                return "not-persuasive-with-modification";
            return "?";
        }

        public String toSystem(ArtifactAssessmentDisposition code) {
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
        ArtifactAssessmentContentExtension content = new ArtifactAssessmentContentExtension();
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
        ArtifactAssessmentContentExtension content = new ArtifactAssessmentContentExtension();
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
        ArtifactAssessmentContentExtension content = new ArtifactAssessmentContentExtension();
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
                    .filter(extension -> extension.getUrl().equals(ArtifactAssessmentContentExtension.RELATEDARTIFACT)
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
            content.addExtension(ArtifactAssessmentContentExtension.RELATEDARTIFACT, newRelatedArtifact);
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
        boolean infoTypeCorrect = false;
        boolean summaryCorrect = false;
        boolean citationRelatedArtifactCorrect = false;
        boolean derivedFromRelatedArtifactCorrect = false;
        boolean authorCorrect = false;
        boolean artifactCorrect = false;
        int contentIndex = findIndex(CONTENT, null, this.getExtension());
        if (contentIndex != -1) {
            var contentExt = this.getExtension().get(contentIndex);
            int infoTypeIndex = findIndex(ArtifactAssessmentContentExtension.INFOTYPE, null, contentExt.getExtension());
            if (infoTypeIndex != -1) {
                var infoTypeExt = contentExt.getExtension().get(infoTypeIndex);
                infoTypeCorrect = ((CodeType) infoTypeExt.getValue()).getCode().equals(artifactAssessmentType);
            }
            int summaryIndex = findIndex(ArtifactAssessmentContentExtension.SUMMARY, null, contentExt.getExtension());
            if (summaryIndex != -1) {
                var summaryExt = contentExt.getExtension().get(summaryIndex);
                summaryCorrect = ((StringType) summaryExt.getValue()).getValue().equals(artifactAssessmentSummary);
            }
            var relatedArtifactList = contentExt.getExtension().stream()
                    .filter(e -> e.getUrl().equals(ArtifactAssessmentContentExtension.RELATEDARTIFACT))
                    .collect(Collectors.toList());
            if (relatedArtifactList.size() > 0) {
                var maybeCitation = relatedArtifactList.stream()
                        .filter(ext ->
                                ((RelatedArtifact) ext.getValue()).getType().equals(RelatedArtifactType.CITATION))
                        .findAny();
                var maybeDerivedFrom = relatedArtifactList.stream()
                        .filter(ext ->
                                ((RelatedArtifact) ext.getValue()).getType().equals(RelatedArtifactType.DERIVEDFROM))
                        .findAny();
                if (maybeCitation.isPresent()) {
                    var citation = maybeCitation.get();
                    citationRelatedArtifactCorrect = ((RelatedArtifact) citation.getValue())
                            .getResource()
                            .equals(artifactAssessmentRelatedArtifact);
                }
                if (maybeDerivedFrom.isPresent()) {
                    var derivedFrom = maybeDerivedFrom.get();
                    derivedFromRelatedArtifactCorrect = ((RelatedArtifact) derivedFrom.getValue())
                            .getResource()
                            .equals(derivedFromRelatedArtifactUrl);
                }
            }
            int authorIndex = findIndex(ArtifactAssessmentContentExtension.AUTHOR, null, contentExt.getExtension());
            if (authorIndex != -1) {
                var authorExt = contentExt.getExtension().get(authorIndex);
                authorCorrect =
                        ((Reference) authorExt.getValue()).getReference().equals(artifactAssessmentAuthor);
            }
        }
        int artifactIndex = findIndex(ARTIFACT, null, this.getExtension());
        if (artifactIndex != -1) {
            var artifactExt = this.getExtension().get(artifactIndex);
            artifactCorrect =
                    ((Reference) artifactExt.getValue()).getReference().equals(artifactAssessmentTargetReference);
        }
        return artifactCorrect
                && infoTypeCorrect
                && summaryCorrect
                && citationRelatedArtifactCorrect
                && derivedFromRelatedArtifactCorrect
                && authorCorrect;
    }

    public boolean isValidArtifactComment() {
        boolean infoTypeExists = false;
        boolean summaryExists = false;
        boolean relatedArtifactExists = false;
        boolean authorExists = false;
        boolean dateExists = findIndex(DATE, null, this.getExtension()) != -1;
        boolean artifactExists = findIndex(ARTIFACT, null, this.getExtension()) != -1;
        int contentIndex = findIndex(CONTENT, null, this.getExtension());
        if (contentIndex != -1) {
            var content = this.getExtension().get(contentIndex);
            infoTypeExists = findIndex(ArtifactAssessmentContentExtension.INFOTYPE, null, content.getExtension()) != -1;
            summaryExists = findIndex(ArtifactAssessmentContentExtension.SUMMARY, null, content.getExtension()) != -1;
            relatedArtifactExists =
                    findIndex(ArtifactAssessmentContentExtension.RELATEDARTIFACT, null, content.getExtension()) != -1;
            authorExists = findIndex(ArtifactAssessmentContentExtension.AUTHOR, null, content.getExtension()) != -1;
        }
        return (infoTypeExists || summaryExists || relatedArtifactExists || authorExists)
                && dateExists
                && artifactExists;
    }

    List<ArtifactAssessmentContentExtension> getContent() {
        return this.getExtension().stream()
                .filter(ext -> ext.getUrl().equals(CONTENT))
                .map(ext -> (ArtifactAssessmentContentExtension) ext)
                .collect(Collectors.toList());
    }

    public ArtifactAssessment setArtifactExtension(CanonicalType target) {
        if (target != null && target.getValue() != null) {
            int index = findIndex(ARTIFACT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentArtifactExtension(target));
            } else {
                this.addExtension(new ArtifactAssessmentArtifactExtension(target));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactExtension(Reference target) {
        if (target != null && target.getReference() != null) {
            int index = findIndex(ARTIFACT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentArtifactExtension(target));
            } else {
                this.addExtension(new ArtifactAssessmentArtifactExtension(target));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactExtension(UriType target) {
        if (target != null && target.getValue() != null) {
            int index = findIndex(ARTIFACT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentArtifactExtension(target));
            } else {
                this.addExtension(new ArtifactAssessmentArtifactExtension(target));
            }
        }
        return this;
    }

    public ArtifactAssessment setDateExtension(DateTimeType date) {
        if (date != null) {
            int index = findIndex(DATE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentDateExtension(date));
            } else {
                this.addExtension(new ArtifactAssessmentDateExtension(date));
            }
        }
        return this;
    }

    public ArtifactAssessment setLastReviewDateExtension(DateType reviewDate) {
        if (reviewDate != null) {
            int index = findIndex(LAST_REVIEW_DATE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentLastReviewDateExtension(reviewDate));
            } else {
                this.addExtension(new ArtifactAssessmentLastReviewDateExtension(reviewDate));
            }
        }
        return this;
    }

    public ArtifactAssessment setApprovalDateExtension(DateType approvalDate) {
        if (approvalDate != null) {
            int index = findIndex(APPROVAL_DATE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentApprovalDateExtension(approvalDate));
            } else {
                this.addExtension(new ArtifactAssessmentApprovalDateExtension(approvalDate));
            }
        }
        return this;
    }

    public ArtifactAssessment setTitleExtension(MarkdownType title) {
        if (title != null) {
            int index = findIndex(TITLE, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentTitleExtension(title));
            } else {
                this.addExtension(new ArtifactAssessmentTitleExtension(title));
            }
        }
        return this;
    }

    public ArtifactAssessment setCopyrightExtension(MarkdownType copyright) {
        if (copyright != null) {
            int index = findIndex(COPYRIGHT, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentCopyrightExtension(copyright));
            } else {
                this.addExtension(new ArtifactAssessmentCopyrightExtension(copyright));
            }
        }
        return this;
    }

    public ArtifactAssessment setCiteAsExtension(MarkdownType citeAs) {
        if (citeAs != null) {
            int index = findIndex(CITEAS, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentCiteAsExtension(citeAs));
            } else {
                this.addExtension(new ArtifactAssessmentCiteAsExtension(citeAs));
            }
        }
        return this;
    }

    public ArtifactAssessment setCiteAsExtension(Reference citeAs) {
        if (citeAs != null) {
            int index = findIndex(CITEAS, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentCiteAsExtension(citeAs));
            } else {
                this.addExtension(new ArtifactAssessmentCiteAsExtension(citeAs));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactAssessmentWorkflowStatusExtension(
            Enumeration<ArtifactAssessmentWorkflowStatus> status) {
        if (status != null) {
            int index = findIndex(WORKFLOW_STATUS, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentWorkflowStatusExtension(status));
            } else {
                this.addExtension(new ArtifactAssessmentWorkflowStatusExtension(status));
            }
        }
        return this;
    }

    public ArtifactAssessment setArtifactAssessmentDispositionExtension(
            Enumeration<ArtifactAssessmentDisposition> status) {
        if (status != null) {
            int index = findIndex(DISPOSITION, null, this.getExtension());
            if (index != -1) {
                this.extension.set(index, new ArtifactAssessmentDispositionExtension(status));
            } else {
                this.addExtension(new ArtifactAssessmentDispositionExtension(status));
            }
        }
        return this;
    }

    private int findIndex(String url, Type value, List<Extension> extensions) {
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

        ArtifactAssessmentContentExtension setInfoType(CodeType infoType) throws FHIRException {
            if (infoType != null) {
                int index = findIndex(INFOTYPE, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(index, new ArtifactAssessmentContentInformationTypeExtension(infoType));
                } else {
                    this.addExtension(new ArtifactAssessmentContentInformationTypeExtension(infoType));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension setSummary(MarkdownType summary) {
            if (summary != null) {
                int index = findIndex(SUMMARY, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(index, new ArtifactAssessmentContentSummaryExtension(summary));
                } else {
                    this.addExtension(new ArtifactAssessmentContentSummaryExtension(summary));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension addRelatedArtifact(CanonicalType reference, RelatedArtifactType type) {
            if (reference != null) {
                RelatedArtifact newRelatedArtifact = new RelatedArtifact();
                newRelatedArtifact.setType(type);
                newRelatedArtifact.setResourceElement(reference);
                int index = findIndex(RELATEDARTIFACT, newRelatedArtifact, this.getExtension());
                if (index == -1) {
                    this.addExtension(new ArtifactAssessmentContentRelatedArtifactExtension(newRelatedArtifact));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension addComponent(ArtifactAssessmentContentExtension component) {
            if (component != null) {
                this.addExtension(new ArtifactAssessmentContentComponentExtension(component));
            }
            return this;
        }

        ArtifactAssessmentContentExtension setAuthorExtension(Reference author) {
            if (author != null) {
                int index = findIndex(AUTHOR, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(index, new ArtifactAssessmentContentAuthorExtension(author));
                } else {
                    this.addExtension(new ArtifactAssessmentContentAuthorExtension(author));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension setQuantityExtension(Quantity quantity) {
            if (quantity != null) {
                int index = findIndex(QUANTITY, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(index, new ArtifactAssessmentContentQuantityExtension(quantity));
                } else {
                    this.addExtension(new ArtifactAssessmentContentQuantityExtension(quantity));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension setTypeExtension(CodeableConcept type) {
            if (type != null) {
                int index = findIndex(TYPE, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(index, new ArtifactAssessmentContentTypeExtension(type));
                } else {
                    this.addExtension(new ArtifactAssessmentContentTypeExtension(type));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension setFreeToShareExtension(BooleanType freeToShare) {
            if (freeToShare != null) {
                int index = findIndex(FREETOSHARE, null, this.getExtension());
                if (index != -1) {
                    this.extension.set(index, new ArtifactAssessmentContentFreeToShareExtension(freeToShare));
                } else {
                    this.addExtension(new ArtifactAssessmentContentFreeToShareExtension(freeToShare));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension addClassifierExtension(CodeableConcept classifier) {
            if (classifier != null) {
                int index = findIndex(CLASSIFIER, classifier, this.getExtension());
                if (index == -1) {
                    this.addExtension(new ArtifactAssessmentContentClassifierExtension(classifier));
                }
            }
            return this;
        }

        ArtifactAssessmentContentExtension addPathExtension(UriType path) {
            if (path != null) {
                int index = findIndex(PATH, path, this.getExtension());
                if (index == -1) {
                    this.addExtension(new ArtifactAssessmentContentPathExtension(path));
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
                ArtifactAssessmentContentInformationType.fromCode(informationTypeCode.getValue());
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
            public ArtifactAssessmentContentComponentExtension(ArtifactAssessmentContentExtension contentExtension) {
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
        public ArtifactAssessmentWorkflowStatusExtension(Enumeration<ArtifactAssessmentWorkflowStatus> status) {
            super(WORKFLOW_STATUS, status);
        }
    }

    @DatatypeDef(name = "ArtifactAssessmentDispositionExtension", isSpecialization = true, profileOf = Extension.class)
    private class ArtifactAssessmentDispositionExtension extends Extension {
        public ArtifactAssessmentDispositionExtension(Enumeration<ArtifactAssessmentDisposition> disposition) {
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
