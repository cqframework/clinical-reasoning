package org.opencds.cqf.fhir.utility.dstu3

import ca.uhn.fhir.model.api.annotation.Binding
import ca.uhn.fhir.model.api.annotation.DatatypeDef
import ca.uhn.fhir.model.api.annotation.ResourceDef
import java.util.*
import java.util.stream.Collectors
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.dstu3.model.Enumeration
import org.hl7.fhir.exceptions.FHIRException

@ResourceDef(
    id = "ArtifactAssessment",
    profile = "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessment",
)
class ArtifactAssessment : Basic {
    enum class ArtifactAssessmentContentInformationType {
        /** A comment on the artifact */
        COMMENT,

        /** A classifier of the artifact */
        CLASSIFIER,

        /** A rating of the artifact */
        RATING,

        /** A container for multiple components */
        CONTAINER,

        /** A response to a comment */
        RESPONSE,

        /** A change request for the artifact */
        CHANGEREQUEST,

        /** added to help the parsers with the generic types */
        NULL;

        fun toCode(): String? {
            when (this) {
                COMMENT -> return "comment"
                CLASSIFIER -> return "classifier"
                RATING -> return "rating"
                CONTAINER -> return "container"
                RESPONSE -> return "response"
                CHANGEREQUEST -> return "change-request"
                NULL -> return null
                else -> return "?"
            }
        }

        val system: String?
            get() {
                when (this) {
                    COMMENT,
                    CLASSIFIER,
                    RATING,
                    CONTAINER,
                    RESPONSE,
                    CHANGEREQUEST ->
                        return "http://hl7.org/fhir/ValueSet/artifactassessment-information-type"
                    NULL -> return null
                    else -> return "?"
                }
            }

        val definition: String?
            get() {
                when (this) {
                    COMMENT -> return "A comment on the artifact."
                    CLASSIFIER -> return "A classifier of the artifact."
                    RATING -> return "A rating  of the artifact."
                    CONTAINER -> return "A container for multiple components."
                    RESPONSE -> return "A response to a comment."
                    CHANGEREQUEST -> return "A change request for the artifact."
                    NULL -> return null
                    else -> return "?"
                }
            }

        val display: String?
            get() {
                when (this) {
                    COMMENT -> return "Comment"
                    CLASSIFIER -> return "Classifier"
                    RATING -> return "Rating"
                    CONTAINER -> return "Container"
                    RESPONSE -> return "Response"
                    CHANGEREQUEST -> return "Change Request"
                    NULL -> return null
                    else -> return "?"
                }
            }

        companion object {
            @JvmStatic
            @Throws(FHIRException::class)
            fun fromCode(codeString: String?): ArtifactAssessmentContentInformationType? {
                if (StringUtils.isBlank(codeString)) {
                    return null
                }
                when (codeString) {
                    "comment" -> return COMMENT
                    "classifier" -> return CLASSIFIER
                    "rating" -> return RATING
                    "container" -> return CONTAINER
                    "response" -> return RESPONSE
                    "change-request" -> return CHANGEREQUEST

                    else ->
                        if (Configuration.isAcceptInvalidEnums()) {
                            return null
                        } else {
                            throw FHIRException("Unknown ArtifactAssessment '" + codeString + "'")
                        }
                }
            }
        }
    }

    class ArtifactAssessmentContentInformationTypeEnumFactory :
        EnumFactory<ArtifactAssessmentContentInformationType> {
        @Throws(IllegalArgumentException::class)
        override fun fromCode(codeString: String?): ArtifactAssessmentContentInformationType? {
            return ArtifactAssessmentContentInformationType.Companion.fromCode(codeString)
        }

        @Throws(FHIRException::class)
        fun fromType(code: Base?): Enumeration<ArtifactAssessmentContentInformationType?>? {
            if (code == null) return null
            if (code.isEmpty()) return Enumeration<ArtifactAssessmentContentInformationType?>(this)
            return Enumeration<ArtifactAssessmentContentInformationType?>(
                this,
                ArtifactAssessmentContentInformationType.Companion.fromCode(
                    (code as PrimitiveType<*>).asStringValue()
                ),
            )
        }

        override fun toCode(code: ArtifactAssessmentContentInformationType): String? {
            return code.toCode()
        }

        override fun toSystem(code: ArtifactAssessmentContentInformationType): String? {
            return code.system
        }
    }

    enum class ArtifactAssessmentContentClassifier {
        /** High quality evidence. */
        HIGH,

        /** Moderate quality evidence. */
        MODERATE,

        /** Low quality evidence. */
        LOW,

        /** Very low quality evidence */
        VERY_LOW,

        /** No serious concern. */
        NO_CONCERN,

        /** Serious concern. */
        SERIOUS_CONCERN,

        /** Very serious concern. */
        VERY_SERIOUS_CONCERN,

        /** Extremely serious concern. */
        EXTREMELY_SERIOUS_CONCERN,

        /** Possible reason for increasing quality rating was checked and found to be present. */
        PRESENT,

        /** Possible reason for increasing quality rating was checked and found to be absent. */
        ABSENT,

        /** No change to quality rating. */
        NO_CHANGE,

        /** Reduce quality rating by 1. */
        DOWNCODE1,

        /** Reduce quality rating by 2. */
        DOWNCODE2,

        /** Reduce quality rating by 3. */
        DOWNCODE3,

        /** Increase quality rating by 1. */
        UPCODE1,

        /** Increase quality rating by 2 */
        UPCODE2,

        /** added to help the parsers with the generic types */
        NULL;

        fun toCode(): String? {
            when (this) {
                HIGH -> return "high"
                MODERATE -> return "moderate"
                LOW -> return "low"
                VERY_LOW -> return "very-low"
                NO_CONCERN -> return "no-concern"
                SERIOUS_CONCERN -> return "serious-concern"
                VERY_SERIOUS_CONCERN -> return "very-serious-concern"
                EXTREMELY_SERIOUS_CONCERN -> return "extremely-serious-concern"
                PRESENT -> return "present"
                ABSENT -> return "absent"
                NO_CHANGE -> return "no-change"
                DOWNCODE1 -> return "downcode1"
                DOWNCODE2 -> return "downcode2"
                DOWNCODE3 -> return "downcode3"
                UPCODE1 -> return "upcode1"
                UPCODE2 -> return "upcode2"
                NULL -> return null
                else -> return "?"
            }
        }

        val system: String?
            get() {
                when (this) {
                    HIGH,
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
                    UPCODE2 -> return "http://terminology.hl7.org/CodeSystem/certainty-rating"
                    NULL -> return null
                    else -> return "?"
                }
            }

        val definition: String?
            get() {
                when (this) {
                    HIGH -> return "High quality evidence."
                    MODERATE -> return "Moderate quality evidence."
                    LOW -> return "Low quality evidence."
                    VERY_LOW -> return "Very low quality evidence."
                    NO_CONCERN -> return "No serious concern."
                    SERIOUS_CONCERN -> return "Serious concern."
                    VERY_SERIOUS_CONCERN -> return "Very serious concern."
                    EXTREMELY_SERIOUS_CONCERN -> return "Extremely serious concern."
                    PRESENT ->
                        return "Possible reason for increasing quality rating was checked and found to be present."
                    ABSENT ->
                        return "Possible reason for increasing quality rating was checked and found to be absent."
                    NO_CHANGE -> return "No change to quality rating."
                    DOWNCODE1 -> return "Reduce quality rating by 1."
                    DOWNCODE2 -> return "Reduce quality rating by 2."
                    DOWNCODE3 -> return "Reduce quality rating by 3."
                    UPCODE1 -> return "Increase quality rating by 1."
                    UPCODE2 -> return "Increase quality rating by 2."
                    NULL -> return null
                    else -> return "?"
                }
            }

        val display: String?
            get() {
                when (this) {
                    HIGH -> return "High quality"
                    MODERATE -> return "Moderate quality"
                    LOW -> return "Low quality"
                    VERY_LOW -> return "Very low quality"
                    NO_CONCERN -> return "No serious concern"
                    SERIOUS_CONCERN -> return "Serious concern"
                    VERY_SERIOUS_CONCERN -> return "Very serious concern"
                    EXTREMELY_SERIOUS_CONCERN -> return "Extremely serious concern"
                    PRESENT -> return "Present"
                    ABSENT -> return "Absent"
                    NO_CHANGE -> return "No change to rating"
                    DOWNCODE1 -> return "Reduce rating: -1"
                    DOWNCODE2 -> return "Reduce rating: -2"
                    DOWNCODE3 -> return "Reduce rating: -3"
                    UPCODE1 -> return "Increase rating: +1"
                    UPCODE2 -> return "Increase rating: +2"
                    NULL -> return null
                    else -> return "?"
                }
            }

        companion object {
            @JvmStatic
            @Throws(FHIRException::class)
            fun fromCode(codeString: String?): ArtifactAssessmentContentClassifier? {
                if (StringUtils.isBlank(codeString)) {
                    return null
                }
                when (codeString) {
                    "high" -> return HIGH
                    "moderate" -> return MODERATE
                    "low" -> return LOW
                    "very-low" -> return VERY_LOW
                    "no-concern" -> return NO_CONCERN
                    "serious-concern" -> return SERIOUS_CONCERN
                    "very-serious-concern" -> return VERY_SERIOUS_CONCERN
                    "extremely-serious-concern" -> return EXTREMELY_SERIOUS_CONCERN
                    "present" -> return PRESENT
                    "absent" -> return ABSENT
                    "no-change" -> return NO_CHANGE
                    "downcode1" -> return DOWNCODE1
                    "downcode2" -> return DOWNCODE2
                    "downcode3" -> return DOWNCODE3
                    "upcode1" -> return UPCODE1
                    "upcode2" -> return UPCODE2
                    else ->
                        if (Configuration.isAcceptInvalidEnums()) {
                            return null
                        } else {
                            throw FHIRException("Unknown ArtifactAssessment '" + codeString + "'")
                        }
                }
            }
        }
    }

    class ArtifactAssessmentContentClassifierEnumFactory :
        EnumFactory<ArtifactAssessmentContentClassifier> {
        @Throws(IllegalArgumentException::class)
        override fun fromCode(codeString: String?): ArtifactAssessmentContentClassifier? {
            return ArtifactAssessmentContentClassifier.Companion.fromCode(codeString)
        }

        @Throws(FHIRException::class)
        fun fromType(code: Base?): Enumeration<ArtifactAssessmentContentClassifier?>? {
            if (code == null) {
                return null
            }
            if (code.isEmpty()) {
                return Enumeration<ArtifactAssessmentContentClassifier?>(this)
            }
            return Enumeration<ArtifactAssessmentContentClassifier?>(
                this,
                ArtifactAssessmentContentClassifier.Companion.fromCode(
                    (code as PrimitiveType<*>).asStringValue()
                ),
            )
        }

        override fun toCode(code: ArtifactAssessmentContentClassifier): String? {
            return code.toCode()
        }

        override fun toSystem(code: ArtifactAssessmentContentClassifier): String? {
            return code.system
        }
    }

    enum class ArtifactAssessmentContentType

    enum class ArtifactAssessmentWorkflowStatus {
        /**
         * The comment has been submitted, but the responsible party has not yet been determined, or
         * the responsible party has not yet determined the next steps to be taken.
         */
        SUBMITTED,

        /**
         * The comment has been triaged, meaning the responsible party has been determined and next
         * steps have been identified to address the comment.
         */
        TRIAGED,

        /**
         * The comment is waiting for input from a specific party before next steps can be taken.
         */
        WAITINGFORINPUT,

        /** The comment has been resolved and no changes resulted from the resolution */
        RESOLVEDNOCHANGE,

        /** The comment has been resolved and changes are required to address the comment */
        RESOLVEDCHANGEREQUIRED,

        /**
         * The comment is acceptable, but resolution of the comment and application of any
         * associated changes have been deferred
         */
        DEFERRED,

        /** The comment is a duplicate of another comment already received */
        DUPLICATE,

        /** The comment is resolved and any necessary changes have been applied */
        APPLIED,

        /**
         * The necessary changes to the artifact have been published in a new version of the
         * artifact
         */
        PUBLISHED,

        /** added to help the parsers with the generic types */
        NULL;

        fun toCode(): String {
            when (this) {
                SUBMITTED -> return "submitted"
                TRIAGED -> return "triaged"
                WAITINGFORINPUT -> return "waiting-for-input"
                RESOLVEDNOCHANGE -> return "resolved-no-change"
                RESOLVEDCHANGEREQUIRED -> return "resolved-change-required"
                DEFERRED -> return "deferred"
                DUPLICATE -> return "duplicate"
                APPLIED -> return "applied"
                PUBLISHED -> return "published"
                else -> return "?"
            }
        }

        val system: String
            get() {
                if (this == NULL) {
                    return "?"
                } else {
                    return "http://hl7.org/fhir/artifactassessment-workflow-status"
                }
            }

        val definition: String
            get() {
                when (this) {
                    SUBMITTED ->
                        return "The comment has been submitted, but the responsible party has not yet been determined, or the responsible party has not yet determined the next steps to be taken."
                    TRIAGED ->
                        return "The comment has been triaged, meaning the responsible party has been determined and next steps have been identified to address the comment."
                    WAITINGFORINPUT ->
                        return "The comment is waiting for input from a specific party before next steps can be taken."
                    RESOLVEDNOCHANGE ->
                        return "The comment has been resolved and no changes resulted from the resolution"
                    RESOLVEDCHANGEREQUIRED ->
                        return "The comment has been resolved and changes are required to address the comment"
                    DEFERRED ->
                        return "The comment is acceptable, but resolution of the comment and application of any associated changes have been deferred"
                    DUPLICATE ->
                        return "The comment is a duplicate of another comment already received"
                    APPLIED ->
                        return "The comment is resolved and any necessary changes have been applied"
                    PUBLISHED ->
                        return "The necessary changes to the artifact have been published in a new version of the artifact"
                    else -> return "?"
                }
            }

        val display: String
            get() {
                when (this) {
                    SUBMITTED -> return "Submitted"
                    TRIAGED -> return "Triaged"
                    WAITINGFORINPUT -> return "Waiting for Input"
                    RESOLVEDNOCHANGE -> return "Resolved - No Change"
                    RESOLVEDCHANGEREQUIRED -> return "Resolved - Change Required"
                    DEFERRED -> return "Deferred"
                    DUPLICATE -> return "Duplicate"
                    APPLIED -> return "Applied"
                    PUBLISHED -> return "Published"
                    else -> return "?"
                }
            }

        companion object {
            @JvmStatic
            @Throws(FHIRException::class)
            fun fromCode(codeString: String?): ArtifactAssessmentWorkflowStatus? {
                if (StringUtils.isBlank(codeString)) {
                    return null
                }
                when (codeString) {
                    "submitted" -> return SUBMITTED
                    "triaged" -> return TRIAGED
                    "waiting-for-input" -> return WAITINGFORINPUT
                    "resolved-no-change" -> return RESOLVEDNOCHANGE
                    "resolved-change-required" -> return RESOLVEDCHANGEREQUIRED
                    "deferred" -> return DEFERRED
                    "duplicate" -> return DUPLICATE
                    "applied" -> return APPLIED
                    "published" -> return PUBLISHED

                    else ->
                        if (Configuration.isAcceptInvalidEnums()) {
                            return null
                        } else {
                            throw FHIRException(
                                "Unknown ArtifactAssessmentWorkflowStatus code '" + codeString + "'"
                            )
                        }
                }
            }
        }
    }

    class ArtifactAssessmentWorkflowStatusEnumFactory :
        EnumFactory<ArtifactAssessmentWorkflowStatus> {
        @Throws(IllegalArgumentException::class)
        override fun fromCode(codeString: String?): ArtifactAssessmentWorkflowStatus? {
            return ArtifactAssessmentWorkflowStatus.Companion.fromCode(codeString)
        }

        @Throws(FHIRException::class)
        fun fromType(code: Base?): Enumeration<ArtifactAssessmentWorkflowStatus?>? {
            if (code == null) {
                return null
            }
            if (code.isEmpty()) {
                return Enumeration<ArtifactAssessmentWorkflowStatus?>(this)
            }
            return Enumeration<ArtifactAssessmentWorkflowStatus?>(
                this,
                ArtifactAssessmentWorkflowStatus.Companion.fromCode(
                    (code as PrimitiveType<*>).asStringValue()
                ),
            )
        }

        override fun toCode(code: ArtifactAssessmentWorkflowStatus): String {
            return code.toCode()
        }

        override fun toSystem(code: ArtifactAssessmentWorkflowStatus): String {
            return code.system
        }
    }

    enum class ArtifactAssessmentDisposition {
        /** The comment is unresolved */
        UNRESOLVED,

        /** The comment is not persuasive (rejected in full) */
        NOTPERSUASIVE,

        /** The comment is persuasive (accepted in full) */
        PERSUASIVE,

        /** The comment is persuasive with modification (partially accepted) */
        PERSUASIVEWITHMODIFICATION,

        /** The comment is not persuasive with modification (partially rejected) */
        NOTPERSUASIVEWITHMODIFICATION,

        /** added to help the parsers with the generic types */
        NULL;

        fun toCode(): String {
            when (this) {
                UNRESOLVED -> return "unresolved"
                NOTPERSUASIVE -> return "not-persuasive"
                PERSUASIVE -> return "persuasive"
                PERSUASIVEWITHMODIFICATION -> return "persuasive-with-modification"
                NOTPERSUASIVEWITHMODIFICATION -> return "not-persuasive-with-modification"
                else -> return "?"
            }
        }

        val system: String
            get() {
                if (this == NULL) {
                    return "?"
                } else {
                    return "http://hl7.org/fhir/artifactassessment-disposition"
                }
            }

        val definition: String
            get() {
                when (this) {
                    UNRESOLVED -> return "The comment is unresolved"
                    NOTPERSUASIVE -> return "The comment is not persuasive (rejected in full)"
                    PERSUASIVE -> return "The comment is persuasive (accepted in full)"
                    PERSUASIVEWITHMODIFICATION ->
                        return "The comment is persuasive with modification (partially accepted)"
                    NOTPERSUASIVEWITHMODIFICATION ->
                        return "The comment is not persuasive with modification (partially rejected)"
                    else -> return "?"
                }
            }

        val display: String
            get() {
                when (this) {
                    UNRESOLVED -> return "Unresolved"
                    NOTPERSUASIVE -> return "Not Persuasive"
                    PERSUASIVE -> return "Persuasive"
                    PERSUASIVEWITHMODIFICATION -> return "Persuasive with Modification"
                    NOTPERSUASIVEWITHMODIFICATION -> return "Not Persuasive with Modification"
                    else -> return "?"
                }
            }

        companion object {
            @JvmStatic
            @Throws(FHIRException::class)
            fun fromCode(codeString: String?): ArtifactAssessmentDisposition? {
                if (StringUtils.isBlank(codeString)) {
                    return null
                }
                when (codeString) {
                    "unresolved" -> return UNRESOLVED
                    "not-persuasive" -> return NOTPERSUASIVE
                    "persuasive" -> return PERSUASIVE
                    "persuasive-with-modification" -> return PERSUASIVEWITHMODIFICATION
                    "not-persuasive-with-modification" -> return NOTPERSUASIVEWITHMODIFICATION

                    else ->
                        if (Configuration.isAcceptInvalidEnums()) {
                            return null
                        } else {
                            throw FHIRException(
                                "Unknown ArtifactAssessmentDisposition code '" + codeString + "'"
                            )
                        }
                }
            }
        }
    }

    class ArtifactAssessmentDispositionEnumFactory : EnumFactory<ArtifactAssessmentDisposition> {
        @Throws(IllegalArgumentException::class)
        override fun fromCode(codeString: String?): ArtifactAssessmentDisposition? {
            return ArtifactAssessmentDisposition.Companion.fromCode(codeString)
        }

        @Throws(FHIRException::class)
        fun fromType(code: Base?): Enumeration<ArtifactAssessmentDisposition?>? {
            if (code == null) {
                return null
            }
            if (code.isEmpty()) {
                return Enumeration<ArtifactAssessmentDisposition?>(this)
            }
            return Enumeration<ArtifactAssessmentDisposition?>(
                this,
                ArtifactAssessmentDisposition.Companion.fromCode(
                    (code as PrimitiveType<*>).asStringValue()
                ),
            )
        }

        override fun toCode(code: ArtifactAssessmentDisposition): String {
            return code.toCode()
        }

        override fun toSystem(code: ArtifactAssessmentDisposition): String {
            return code.system
        }
    }

    constructor() : super()

    constructor(artifact: Reference?) : super() {
        this.setArtifactExtension(artifact)
    }

    constructor(artifact: UriType?) : super() {
        this.setArtifactExtension(artifact)
    }

    @Throws(FHIRException::class)
    fun createArtifactComment(
        type: CodeType?,
        targetReference: Reference?,
        derivedFromUri: Optional<UriType>,
        text: Optional<MarkdownType>,
        reference: Optional<UriType>,
        user: Optional<Reference>,
    ): ArtifactAssessment {
        val content = ArtifactAssessmentContentExtension()
        if (type != null) {
            content.setInfoType(type)
        }
        if (text.isPresent() && !StringUtils.isBlank(text.get().getValue())) {
            content.setSummary(text.get())
        }
        if (reference.isPresent() && !StringUtils.isBlank(reference.get().getValue())) {
            content.addRelatedArtifact(
                reference.get(),
                RelatedArtifact.RelatedArtifactType.CITATION,
            )
        }
        if (user.isPresent() && !StringUtils.isBlank(user.get().getReference())) {
            content.setAuthorExtension(user.get())
        }
        if (derivedFromUri.isPresent() && !StringUtils.isBlank(derivedFromUri.get().getValue())) {
            content.addRelatedArtifact(
                derivedFromUri.get(),
                RelatedArtifact.RelatedArtifactType.DERIVEDFROM,
            )
        }
        this.addExtension(content)
        setDateExtension(DateTimeType(Date()))
        if (targetReference != null && !StringUtils.isBlank(targetReference.getReference())) {
            this.setArtifactExtension(targetReference)
        }
        return this
    }

    @Throws(FHIRException::class)
    fun createArtifactComment(
        type: CodeType?,
        targetReference: UriType?,
        derivedFromUri: Optional<UriType>,
        text: Optional<MarkdownType>,
        reference: Optional<UriType>,
        user: Optional<Reference>,
    ): ArtifactAssessment {
        val content: ArtifactAssessmentContentExtension = ArtifactAssessmentContentExtension()
        if (type != null) {
            content.setInfoType(type)
        }
        if (text.isPresent() && !StringUtils.isBlank(text.get().getValue())) {
            content.setSummary(text.get())
        }
        if (reference.isPresent() && !StringUtils.isBlank(reference.get().getValue())) {
            content.addRelatedArtifact(
                reference.get(),
                RelatedArtifact.RelatedArtifactType.CITATION,
            )
        }
        if (user.isPresent() && !StringUtils.isBlank(user.get().getReference())) {
            content.setAuthorExtension(user.get())
        }
        if (derivedFromUri.isPresent() && !StringUtils.isBlank(derivedFromUri.get().getValue())) {
            content.addRelatedArtifact(
                derivedFromUri.get(),
                RelatedArtifact.RelatedArtifactType.DERIVEDFROM,
            )
        }
        this.addExtension(content)
        setDateExtension(DateTimeType(Date()))
        if (targetReference != null && !StringUtils.isBlank(targetReference.getValue())) {
            this.setArtifactExtension(targetReference)
        }
        return this
    }

    val derivedFromContentRelatedArtifact: Optional<RelatedArtifact>
        get() {
            var returnedRelatedArtifact = Optional.empty<RelatedArtifact>()
            val content = Optional.ofNullable<Extension>(this.getExtensionByUrl(CONTENT))
            if (content.isPresent()) {
                val maybeRelatedArtifact =
                    content
                        .get()
                        .getExtension()
                        .stream()
                        .filter { extension: Extension? ->
                            extension!!.getUrl() ==
                                ArtifactAssessmentContentExtension.Companion.RELATEDARTIFACT &&
                                ((extension.getValue() as RelatedArtifact).getType() ==
                                    RelatedArtifact.RelatedArtifactType.DERIVEDFROM)
                        }
                        .findFirst()
                if (maybeRelatedArtifact.isPresent()) {
                    val derivedFromArtifact =
                        maybeRelatedArtifact.get().getValue() as RelatedArtifact
                    returnedRelatedArtifact = Optional.of(derivedFromArtifact)
                }
            }
            return returnedRelatedArtifact
        }

    fun setDerivedFromContentRelatedArtifact(targetUri: UriType): ArtifactAssessment {
        val existingRelatedArtifact = this.derivedFromContentRelatedArtifact
        if (existingRelatedArtifact.isPresent()) {
            val derivedFromArtifact = existingRelatedArtifact.get()
            derivedFromArtifact.setResource(Reference(targetUri.getValue()))
        } else {
            var content = this.getExtensionByUrl(CONTENT)
            if (content == null) {
                content = Extension(CONTENT)
                this.addExtension(content)
            }
            // this is duplicated from the addRelatedArtifact method
            // since otherwise we get ClassCastExceptions when trying
            // to Cast from Basic to ArtifactAssessment or its Extension subclasses
            val newRelatedArtifact = RelatedArtifact()
            newRelatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DERIVEDFROM)
            newRelatedArtifact.setResource(Reference(targetUri.getValue()))
            content.addExtension(
                ArtifactAssessmentContentExtension.Companion.RELATEDARTIFACT,
                newRelatedArtifact,
            )
        }
        return this
    }

    fun setDerivedFromContentRelatedArtifact(targetUri: String?): ArtifactAssessment {
        return setDerivedFromContentRelatedArtifact(UriType(targetUri))
    }

    fun checkArtifactCommentParams(
        artifactAssessmentType: String?,
        artifactAssessmentSummary: String?,
        artifactAssessmentTargetReference: String?,
        artifactAssessmentRelatedArtifact: String?,
        derivedFromRelatedArtifactUrl: String?,
        artifactAssessmentAuthor: String?,
    ): Boolean {
        var contentCorrect = false
        var artifactCorrect = false
        val contentIndex = findIndex(CONTENT, null, this.getExtension())
        if (contentIndex != -1) {
            contentCorrect =
                checkContent(
                    contentIndex,
                    artifactAssessmentType,
                    artifactAssessmentSummary,
                    artifactAssessmentRelatedArtifact,
                    derivedFromRelatedArtifactUrl,
                    artifactAssessmentAuthor,
                )
        }
        val artifactIndex = findIndex(ARTIFACT, null, this.getExtension())
        if (artifactIndex != -1) {
            val artifactExt = this.getExtension().get(artifactIndex)
            artifactCorrect =
                (artifactExt.getValue() as Reference).getReference() ==
                    artifactAssessmentTargetReference
        }
        return contentCorrect && artifactCorrect
    }

    private fun checkContent(
        contentIndex: Int,
        artifactAssessmentType: String?,
        artifactAssessmentSummary: String?,
        artifactAssessmentRelatedArtifact: String?,
        derivedFromRelatedArtifactUrl: String?,
        artifactAssessmentAuthor: String?,
    ): Boolean {
        var infoTypeCorrect = false
        var summaryCorrect = false
        var citationRelatedArtifactCorrect = false
        var derivedFromRelatedArtifactCorrect = false
        var authorCorrect = false
        val contentExt = this.getExtension().get(contentIndex)
        val infoTypeIndex =
            findIndex(
                ArtifactAssessmentContentExtension.Companion.INFOTYPE,
                null,
                contentExt.getExtension(),
            )
        if (infoTypeIndex != -1) {
            val infoTypeExt = contentExt.getExtension().get(infoTypeIndex)
            infoTypeCorrect =
                (infoTypeExt.getValue() as CodeType).getValueAsString() == artifactAssessmentType
        }
        val summaryIndex =
            findIndex(
                ArtifactAssessmentContentExtension.Companion.SUMMARY,
                null,
                contentExt.getExtension(),
            )
        if (summaryIndex != -1) {
            val summaryExt = contentExt.getExtension().get(summaryIndex)
            summaryCorrect =
                (summaryExt.getValue() as StringType).getValue() == artifactAssessmentSummary
        }
        val relatedArtifactList =
            contentExt
                .getExtension()
                .stream()
                .filter { e: Extension? ->
                    e!!.getUrl() == ArtifactAssessmentContentExtension.Companion.RELATEDARTIFACT
                }
                .collect(Collectors.toList())
        if (!relatedArtifactList.isEmpty()) {
            val maybeCitation =
                relatedArtifactList
                    .stream()
                    .filter { ext: Extension? ->
                        (ext!!.getValue() as RelatedArtifact).getType() ==
                            RelatedArtifact.RelatedArtifactType.CITATION
                    }
                    .findAny()
            val maybeDerivedFrom =
                relatedArtifactList
                    .stream()
                    .filter { ext: Extension? ->
                        (ext!!.getValue() as RelatedArtifact).getType() ==
                            RelatedArtifact.RelatedArtifactType.DERIVEDFROM
                    }
                    .findAny()
            if (maybeCitation.isPresent()) {
                val citation = maybeCitation.get()
                citationRelatedArtifactCorrect =
                    ((citation.getValue() as RelatedArtifact).getResource().getReference() ==
                        artifactAssessmentRelatedArtifact)
            }
            if (maybeDerivedFrom.isPresent()) {
                val derivedFrom = maybeDerivedFrom.get()
                derivedFromRelatedArtifactCorrect =
                    ((derivedFrom.getValue() as RelatedArtifact).getResource().getReference() ==
                        derivedFromRelatedArtifactUrl)
            }
        }
        val authorIndex =
            findIndex(
                ArtifactAssessmentContentExtension.Companion.AUTHOR,
                null,
                contentExt.getExtension(),
            )
        if (authorIndex != -1) {
            val authorExt = contentExt.getExtension().get(authorIndex)
            authorCorrect =
                (authorExt.getValue() as Reference).getReference() == artifactAssessmentAuthor
        }

        return infoTypeCorrect &&
            summaryCorrect &&
            citationRelatedArtifactCorrect &&
            derivedFromRelatedArtifactCorrect &&
            authorCorrect
    }

    val isValidArtifactComment: Boolean
        get() {
            var infoTypeExists = false
            var summaryExists = false
            var relatedArtifactExists = false
            var authorExists = false
            val dateExists = findIndex(DATE, null, this.getExtension()) != -1
            val artifactExists = findIndex(ARTIFACT, null, this.getExtension()) != -1
            val contentIndex = findIndex(CONTENT, null, this.getExtension())
            if (contentIndex != -1) {
                val content = this.getExtension().get(contentIndex)
                infoTypeExists =
                    findIndex(
                        ArtifactAssessmentContentExtension.Companion.INFOTYPE,
                        null,
                        content.getExtension(),
                    ) != -1
                summaryExists =
                    findIndex(
                        ArtifactAssessmentContentExtension.Companion.SUMMARY,
                        null,
                        content.getExtension(),
                    ) != -1
                relatedArtifactExists =
                    findIndex(
                        ArtifactAssessmentContentExtension.Companion.RELATEDARTIFACT,
                        null,
                        content.getExtension(),
                    ) != -1
                authorExists =
                    findIndex(
                        ArtifactAssessmentContentExtension.Companion.AUTHOR,
                        null,
                        content.getExtension(),
                    ) != -1
            }
            return (infoTypeExists || summaryExists || relatedArtifactExists || authorExists) &&
                dateExists &&
                artifactExists
        }

    val content: MutableList<ArtifactAssessmentContentExtension?>
        get() =
            this.getExtension()
                .stream()
                .filter { ext: Extension? -> ext!!.getUrl() == CONTENT }
                .map<ArtifactAssessmentContentExtension?> { obj: Extension? ->
                    ArtifactAssessmentContentExtension::class.java.cast(obj)
                }
                .collect(Collectors.toList())

    fun setArtifactExtension(target: Reference?): ArtifactAssessment {
        if (target != null && target.getReference() != null) {
            val index = findIndex(ARTIFACT, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentArtifactExtension(target))
            } else {
                this.addExtension(ArtifactAssessmentArtifactExtension(target))
            }
        }
        return this
    }

    fun setArtifactExtension(target: UriType?): ArtifactAssessment {
        if (target != null && target.getValue() != null) {
            val index = findIndex(ARTIFACT, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentArtifactExtension(target))
            } else {
                this.addExtension(ArtifactAssessmentArtifactExtension(target))
            }
        }
        return this
    }

    fun setDateExtension(date: DateTimeType?): ArtifactAssessment {
        if (date != null) {
            val index = findIndex(DATE, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentDateExtension(date))
            } else {
                this.addExtension(ArtifactAssessmentDateExtension(date))
            }
        }
        return this
    }

    fun setLastReviewDateExtension(reviewDate: DateType?): ArtifactAssessment {
        if (reviewDate != null) {
            val index = findIndex(LAST_REVIEW_DATE, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentLastReviewDateExtension(reviewDate))
            } else {
                this.addExtension(ArtifactAssessmentLastReviewDateExtension(reviewDate))
            }
        }
        return this
    }

    fun setApprovalDateExtension(approvalDate: DateType?): ArtifactAssessment {
        if (approvalDate != null) {
            val index = findIndex(APPROVAL_DATE, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentApprovalDateExtension(approvalDate))
            } else {
                this.addExtension(ArtifactAssessmentApprovalDateExtension(approvalDate))
            }
        }
        return this
    }

    fun setTitleExtension(title: MarkdownType?): ArtifactAssessment {
        if (title != null) {
            val index = findIndex(TITLE, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentTitleExtension(title))
            } else {
                this.addExtension(ArtifactAssessmentTitleExtension(title))
            }
        }
        return this
    }

    fun setCopyrightExtension(copyright: MarkdownType?): ArtifactAssessment {
        if (copyright != null) {
            val index = findIndex(COPYRIGHT, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentCopyrightExtension(copyright))
            } else {
                this.addExtension(ArtifactAssessmentCopyrightExtension(copyright))
            }
        }
        return this
    }

    fun setCiteAsExtension(citeAs: MarkdownType?): ArtifactAssessment {
        if (citeAs != null) {
            val index = findIndex(CITEAS, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentCiteAsExtension(citeAs))
            } else {
                this.addExtension(ArtifactAssessmentCiteAsExtension(citeAs))
            }
        }
        return this
    }

    fun setCiteAsExtension(citeAs: Reference?): ArtifactAssessment {
        if (citeAs != null) {
            val index = findIndex(CITEAS, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentCiteAsExtension(citeAs))
            } else {
                this.addExtension(ArtifactAssessmentCiteAsExtension(citeAs))
            }
        }
        return this
    }

    fun setArtifactAssessmentWorkflowStatusExtension(
        status: Enumeration<ArtifactAssessmentWorkflowStatus?>?
    ): ArtifactAssessment {
        if (status != null) {
            val index = findIndex(WORKFLOW_STATUS, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentWorkflowStatusExtension(status))
            } else {
                this.addExtension(ArtifactAssessmentWorkflowStatusExtension(status))
            }
        }
        return this
    }

    fun setArtifactAssessmentDispositionExtension(
        status: Enumeration<ArtifactAssessmentDisposition?>?
    ): ArtifactAssessment {
        if (status != null) {
            val index = findIndex(DISPOSITION, null, this.getExtension())
            if (index != -1) {
                this.extension.set(index, ArtifactAssessmentDispositionExtension(status))
            } else {
                this.addExtension(ArtifactAssessmentDispositionExtension(status))
            }
        }
        return this
    }

    @DatatypeDef(
        name = "ArtifactAssessmentContentExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    class ArtifactAssessmentContentExtension : Extension(CONTENT) {
        @Throws(FHIRException::class)
        fun setInfoType(infoType: CodeType?): ArtifactAssessmentContentExtension {
            if (infoType != null) {
                val index = findIndex(INFOTYPE, null, this.getExtension())
                if (index != -1) {
                    this.extension.set(
                        index,
                        ArtifactAssessmentContentInformationTypeExtension(infoType),
                    )
                } else {
                    this.addExtension(ArtifactAssessmentContentInformationTypeExtension(infoType))
                }
            }
            return this
        }

        fun setSummary(summary: MarkdownType?): ArtifactAssessmentContentExtension {
            if (summary != null) {
                val index = findIndex(SUMMARY, null, this.getExtension())
                if (index != -1) {
                    this.extension.set(index, ArtifactAssessmentContentSummaryExtension(summary))
                } else {
                    this.addExtension(ArtifactAssessmentContentSummaryExtension(summary))
                }
            }
            return this
        }

        fun addRelatedArtifact(
            reference: UriType?,
            type: RelatedArtifact.RelatedArtifactType?,
        ): ArtifactAssessmentContentExtension {
            if (reference != null) {
                val newRelatedArtifact = RelatedArtifact()
                newRelatedArtifact.setType(type)
                newRelatedArtifact.setResource(Reference(reference.getValue()))
                val index = findIndex(RELATEDARTIFACT, newRelatedArtifact, this.getExtension())
                if (index == -1) {
                    this.addExtension(
                        ArtifactAssessmentContentRelatedArtifactExtension(newRelatedArtifact)
                    )
                }
            }
            return this
        }

        fun addComponent(
            component: ArtifactAssessmentContentExtension?
        ): ArtifactAssessmentContentExtension {
            if (component != null) {
                this.addExtension(ArtifactAssessmentContentComponentExtension(component))
            }
            return this
        }

        fun setAuthorExtension(author: Reference?): ArtifactAssessmentContentExtension {
            if (author != null) {
                val index = findIndex(AUTHOR, null, this.getExtension())
                if (index != -1) {
                    this.extension.set(index, ArtifactAssessmentContentAuthorExtension(author))
                } else {
                    this.addExtension(ArtifactAssessmentContentAuthorExtension(author))
                }
            }
            return this
        }

        fun setQuantityExtension(quantity: Quantity?): ArtifactAssessmentContentExtension {
            if (quantity != null) {
                val index = findIndex(QUANTITY, null, this.getExtension())
                if (index != -1) {
                    this.extension.set(index, ArtifactAssessmentContentQuantityExtension(quantity))
                } else {
                    this.addExtension(ArtifactAssessmentContentQuantityExtension(quantity))
                }
            }
            return this
        }

        fun setTypeExtension(type: CodeableConcept?): ArtifactAssessmentContentExtension {
            if (type != null) {
                val index = findIndex(TYPE, null, this.getExtension())
                if (index != -1) {
                    this.extension.set(index, ArtifactAssessmentContentTypeExtension(type))
                } else {
                    this.addExtension(ArtifactAssessmentContentTypeExtension(type))
                }
            }
            return this
        }

        fun setFreeToShareExtension(freeToShare: BooleanType?): ArtifactAssessmentContentExtension {
            if (freeToShare != null) {
                val index = findIndex(FREETOSHARE, null, this.getExtension())
                if (index != -1) {
                    this.extension.set(
                        index,
                        ArtifactAssessmentContentFreeToShareExtension(freeToShare),
                    )
                } else {
                    this.addExtension(ArtifactAssessmentContentFreeToShareExtension(freeToShare))
                }
            }
            return this
        }

        fun addClassifierExtension(
            classifier: CodeableConcept?
        ): ArtifactAssessmentContentExtension {
            if (classifier != null) {
                val index = findIndex(CLASSIFIER, classifier, this.getExtension())
                if (index == -1) {
                    this.addExtension(ArtifactAssessmentContentClassifierExtension(classifier))
                }
            }
            return this
        }

        fun addPathExtension(path: UriType?): ArtifactAssessmentContentExtension {
            if (path != null) {
                val index = findIndex(PATH, path, this.getExtension())
                if (index == -1) {
                    this.addExtension(ArtifactAssessmentContentPathExtension(path))
                }
            }
            return this
        }

        @DatatypeDef(
            name = "ArtifactAssessmentContentInformationTypeExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentInformationTypeExtension(
            informationTypeCode: CodeType
        ) : Extension(INFOTYPE) {
            init {
                // validate code
                ArtifactAssessmentContentInformationType.Companion.fromCode(
                    informationTypeCode.getValue()
                )
                this.setValue(informationTypeCode)
            }
        }

        @DatatypeDef(
            name = "ArtifactAssessmentContentSummaryExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentSummaryExtension(summary: MarkdownType?) :
            Extension(SUMMARY, summary)

        @DatatypeDef(
            name = "ArtifactAssessmentContentTypeExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentTypeExtension(typeConcept: CodeableConcept?) :
            Extension(TYPE, typeConcept) {
            @Binding(valueSet = "http://hl7.org/fhir/ValueSet/certainty-type")
            protected var value: CodeableConcept? = null
        }

        @DatatypeDef(
            name = "ArtifactAssessmentContentClassifierExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentClassifierExtension(
            classifierConcept: CodeableConcept?
        ) : Extension(CLASSIFIER, classifierConcept) {
            @Binding(valueSet = "http://hl7.org/fhir/ValueSet/certainty-rating")
            protected var value: CodeableConcept? = null
        }

        @DatatypeDef(
            name = "ArtifactAssessmentContentQuantityExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentQuantityExtension(quantity: Quantity?) :
            Extension(QUANTITY, quantity)

        @DatatypeDef(
            name = "ArtifactAssessmentContentAuthorExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentAuthorExtension(author: Reference?) :
            Extension(AUTHOR, author)

        @DatatypeDef(
            name = "ArtifactAssessmentContentPathExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentPathExtension(path: UriType?) :
            Extension(PATH, path)

        @DatatypeDef(
            name = "ArtifactAssessmentContentRelatedArtifactExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentRelatedArtifactExtension(
            relatedArtifact: RelatedArtifact?
        ) : Extension(RELATEDARTIFACT, relatedArtifact)

        @DatatypeDef(
            name = "ArtifactAssessmentContentFreeToShareExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentFreeToShareExtension(freeToShare: BooleanType?) :
            Extension(FREETOSHARE, freeToShare)

        @DatatypeDef(
            name = "ArtifactAssessmentContentComponentExtension",
            isSpecialization = true,
            profileOf = Extension::class,
        )
        private class ArtifactAssessmentContentComponentExtension(
            contentExtension: ArtifactAssessmentContentExtension?
        ) : Extension(COMPONENT, contentExtension)

        companion object {
            const val INFOTYPE: String = "informationType"
            const val SUMMARY: String = "summary"
            const val TYPE: String = "type"
            const val CLASSIFIER: String = "classifier"
            const val QUANTITY: String = "quantity"
            const val AUTHOR: String = "author"
            const val PATH: String = "path"
            const val RELATEDARTIFACT: String = "relatedArtifact"
            const val FREETOSHARE: String = "freeToShare"
            const val COMPONENT: String = "component"
        }
    }

    @DatatypeDef(
        name = "ArtifactAssessmentArtifactExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentArtifactExtension : Extension {
        constructor(target: Reference?) : super(ARTIFACT, target)

        constructor(target: UriType?) : super(ARTIFACT, target)
    }

    @DatatypeDef(
        name = "ArtifactAssessmentWorkflowStatusExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentWorkflowStatusExtension(
        status: Enumeration<ArtifactAssessmentWorkflowStatus?>?
    ) : Extension(WORKFLOW_STATUS, status)

    @DatatypeDef(
        name = "ArtifactAssessmentDispositionExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentDispositionExtension(
        disposition: Enumeration<ArtifactAssessmentDisposition?>?
    ) : Extension(DISPOSITION, disposition)

    @DatatypeDef(
        name = "ArtifactAssessmentLastReviewDateExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentLastReviewDateExtension(lastReviewDate: DateType?) :
        Extension(LAST_REVIEW_DATE, lastReviewDate)

    @DatatypeDef(
        name = "ArtifactAssessmentApprovalDateExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentApprovalDateExtension(approvalDate: DateType?) :
        Extension(APPROVAL_DATE, approvalDate)

    @DatatypeDef(
        name = "ArtifactAssessmentCopyrightExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentCopyrightExtension(copyright: MarkdownType?) :
        Extension(COPYRIGHT, copyright)

    @DatatypeDef(
        name = "ArtifactAssessmentDateExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentDateExtension(date: DateTimeType?) : Extension(DATE, date)

    @DatatypeDef(
        name = "ArtifactAssessmentTitleExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentTitleExtension(title: StringType?) : Extension(TITLE, title)

    @DatatypeDef(
        name = "ArtifactAssessmentCiteAsExtension",
        isSpecialization = true,
        profileOf = Extension::class,
    )
    private class ArtifactAssessmentCiteAsExtension : Extension {
        constructor(citation: Reference?) : super(CITEAS, citation)

        constructor(citation: MarkdownType?) : super(CITEAS, citation)
    }

    companion object {
        private fun findIndex(url: String?, value: Type?, extensions: MutableList<Extension>): Int {
            val existingExtension: Optional<Extension>
            if (value != null) {
                existingExtension =
                    extensions
                        .stream()
                        .filter { e: Extension? -> e!!.getUrl() == url && e.getValue() == value }
                        .findAny()
            } else {
                existingExtension =
                    extensions.stream().filter { e: Extension? -> e!!.getUrl() == url }.findAny()
            }
            if (existingExtension.isPresent()) {
                return extensions.indexOf(existingExtension.get())
            } else {
                return -1
            }
        }

        const val ARTIFACT_COMMENT_EXTENSION_URL: String =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-artifactComment"
        const val CONTENT: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentContent"
        const val ARTIFACT: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentArtifact"
        const val CITEAS: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentCiteAs"
        const val TITLE: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentTitle"
        const val DATE: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentDate"
        const val COPYRIGHT: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentCopyright"
        const val APPROVAL_DATE: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentApprovalDate"
        const val LAST_REVIEW_DATE: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentLastReviewDate"
        const val WORKFLOW_STATUS: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentWorkflowStatus"
        const val DISPOSITION: String =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-artifactAssessmentDisposition"
    }
}
