package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.model.api.Tag

object Constants {
    const val CQL_ENGINE_DEVICE = "http://cqframework.org/fhir/Device/clinical-quality-language"

    const val HL7_FHIR_REST = "hl7-fhir-rest"
    const val HL7_FHIR_FILES = "hl7-fhir-files"
    const val HL7_CQL_FILES = "hl7-cql-files"

    val HL7_FHIR_REST_CODE = Tag(null, HL7_FHIR_REST)
    val HL7_FHIR_FILES_CODE = Tag(null, HL7_FHIR_FILES)
    val HL7_CQL_FILES_CODE = Tag(null, HL7_CQL_FILES)

    const val FHIR_MODEL_URI = "http://hl7.org/fhir"
    const val QDM_MODEL_URI = "urn:healthit-gov:qdm:v5_4"

    val ALIAS_MAP =
        mapOf("FHIR" to FHIR_MODEL_URI, "QUICK" to FHIR_MODEL_URI, "QDM" to QDM_MODEL_URI)

    const val ALT_EXPRESSION_EXT =
        "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-alternativeExpression"

    const val FHIR_TYPE_EXTENSION =
        "http://hl7.org/fhir/StructureDefinition/structuredefinition-fhir-type"
    const val DATA_ABSENT_REASON = "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
    const val DISPLAY_EXTENSION = "http://hl7.org/fhir/StructureDefinition/display"
    const val PERTAINS_TO_GOAL = "http://hl7.org/fhir/StructureDefinition/resource-pertainsToGoal"
    const val REQUEST_DO_NOT_PERFORM =
        "http://hl7.org/fhir/StructureDefinition/request-doNotPerform"
    const val QUESTIONNAIRE_RESPONSE_AUTHOR =
        "http://hl7.org/fhir/StructureDefinition/questionnaireresponse-author"
    const val QUESTIONNAIRE_REFERENCE_PROFILE =
        "http://hl7.org/fhir/StructureDefinition/questionnaire-referenceProfile"
    const val QUESTIONNAIRE_UNIT = "http://hl7.org/fhir/StructureDefinition/questionnaire-unit"
    const val QUESTIONNAIRE_UNIT_VALUE_SET =
        "http://hl7.org/fhir/StructureDefinition/questionnaire-unitValueSet"
    const val VARIABLE_EXTENSION = "http://hl7.org/fhir/StructureDefinition/variable"

    const val CPG_ASSERTION_EXPRESSION =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-assertionExpression"
    const val CPG_FEATURE_EXPRESSION =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-featureExpression"
    const val CPG_FEATURE_EXPRESSION_ELEMENT =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-featureExpressionElement"
    const val CPG_INFERENCE_EXPRESSION =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-inferenceExpression"
    const val CPG_KNOWLEDGE_CAPABILITY =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-knowledgeCapability"
    const val CPG_KNOWLEDGE_REPRESENTATION_LEVEL =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-knowledgeRepresentationLevel"
    const val CPG_QUESTIONNAIRE_GENERATE =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-questionnaire-generate"
    const val CPG_RATIONALE = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-rationale"
    const val CPG_RELATED_ARTIFACT =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-relatedArtifact"
    const val ARTIFACT_RELATED_ARTIFACT =
        "http://hl7.org/fhir/StructureDefinition/artifact-relatedArtifact"
    const val CPG_SERVICE_REQUEST =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-servicerequest"
    const val CPG_STRATEGY = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-strategy"
    const val CPG_INPUT_TEXT = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-input-text"
    const val CPG_INPUT_DESCRIPTION =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-input-description"
    const val CPG_PARAMETER_DEFINITION =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition"

    const val CPG_CUSTOM_ACTIVITY_KIND =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-custom-activity-kind"
    const val CPG_ACTIVITY_KIND = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-activity-kind"
    const val CPG_ACTIVITY_TYPE_CS = "http://hl7.org/fhir/uv/cpg/CodeSystem/cpg-activity-type-cs"

    const val CPG_SUMMARY_FOR = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-summaryFor"
    const val CPG_GENERATED_FOR = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-generatedFor"

    const val CPG_CASE_PLAN_SUMMARY =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-caseplansummary"
    const val CPG_RELATED_SUMMARY_DEFINITION =
        "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-relatedsummarydefinition"

    const val CQF_RESOURCETYPE = "http://hl7.org/fhir/StructureDefinition/cqf-resourceType"

    // DSTU3 CQF Extensions
    const val CQIF_LIBRARY = "http://hl7.org/fhir/StructureDefinition/cqif-library"
    const val CQIF_CQL_EXPRESSION = "http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression"

    const val CQF_APPLICABILITY_BEHAVIOR =
        "http://hl7.org/fhir/StructureDefinition/cqf-applicabilityBehavior"

    const val ARTIFACT_IS_OWNED_EXTENSION_URL =
        "http://hl7.org/fhir/StructureDefinition/artifact-isOwned"
    const val CQF_CQL_OPTIONS = "http://hl7.org/fhir/StructureDefinition/cqf-cqlOptions"
    const val CQF_EXPANSION_PARAMETERS =
        "http://hl7.org/fhir/StructureDefinition/cqf-expansionParameters"
    const val CQF_INPUT_EXPANSION_PARAMETERS =
        "http://hl7.org/fhir/StructureDefinition/cqf-inputParameters"
    const val CQF_EXPRESSION = "http://hl7.org/fhir/StructureDefinition/cqf-expression"
    const val CQF_LIBRARY = "http://hl7.org/fhir/StructureDefinition/cqf-library"
    const val CQF_CALCULATED_VALUE = "http://hl7.org/fhir/StructureDefinition/cqf-calculatedValue"
    const val CQF_FHIR_QUERY_PATTERN =
        "http://hl7.org/fhir/StructureDefinition/cqf-fhirQueryPattern"
    const val CQF_DIRECT_REFERENCE_EXTENSION =
        "http://hl7.org/fhir/StructureDefinition/cqf-directReferenceCode"
    const val CQF_MESSAGES = "http://hl7.org/fhir/StructureDefinition/cqf-messages"
    const val CQF_LOGIC_DEFINITION = "http://hl7.org/fhir/StructureDefinition/cqf-logicDefinition"

    const val CQFM_EFFECTIVE_DATA_REQUIREMENTS =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-effectiveDataRequirements"
    const val CQFM_LOGIC_DEFINITION =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-logicDefinition"
    const val CQFM_SOFTWARE_SYSTEM =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-softwaresystem"
    const val CQFM_INPUT_PARAMETERS =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-inputParameters"
    const val CQFM_COMPONENT =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-component"
    const val CQFM_DIRECT_REFERENCE_EXTENSION =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-directReferenceCode"
    const val CRMI_EFFECTIVE_DATA_REQUIREMENTS =
        "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-effectiveDataRequirements"
    const val CRMI_DEPENDENCY_ROLE =
        "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-dependencyRole"
    const val CRMI_REFERENCE_SOURCE =
        "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-referenceSource"
    const val PACKAGE_SOURCE = "http://hl7.org/fhir/StructureDefinition/package-source"
    const val CRMI_VERSION_1 = "1.0.0"
    const val CRMI_VERSION_2 = "2.0.0"

    const val CRMI_OPERATION_APPROVE = "\$approve"
    const val CRMI_OPERATION_ARTIFACT_DIFF = "\$artifact-diff"
    const val CRMI_OPERATION_DATA_REQUIREMENTS = "\$data-requirements"
    const val CRMI_OPERATION_DELETE = "\$delete"
    const val CRMI_OPERATION_PACKAGE = "\$package"
    const val CRMI_OPERATION_RELEASE = "\$release"
    const val CRMI_OPERATION_RELEASE_MANIFEST = "\$release-manifest"
    const val CRMI_OPERATION_RETIRE = "\$retire"
    const val CRMI_OPERATION_REVISE = "\$revise"
    const val CRMI_OPERATION_WITHDRAW = "\$withdraw"

    const val DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE =
        "http://hl7.org/fhir/us/davinci-dtr/StructureDefinition/dtr-questionnaireresponse-questionnaire"
    const val SDC_QUESTIONNAIRE_HIDDEN =
        "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden"
    const val SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext"
    const val SDC_QUESTIONNAIRE_DEFINITION_EXTRACT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtract"
    const val SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtractValue"
    const val SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemPopulationContext"
    const val SDC_QUESTIONNAIRE_IS_SUBJECT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-isSubject"
    const val SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-prepopulate-subject"
    const val SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaireresponse-isSubject"
    const val SDC_QUESTIONNAIRE_INITIAL_EXPRESSION =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"
    const val SDC_QUESTIONNAIRE_CANDIDATE_EXPRESSION =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-candidateExpression"
    const val SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-lookupQuestionnaire"
    const val SDC_QUESTIONNAIRE_PREPOPULATE =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-prepopulate"
    const val SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-prepopulate-parameter"
    const val SDC_QUESTIONNAIRE_SHORT_TEXT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-shortText"
    const val SDC_QUESTIONNAIRE_OBSERVATION_EXTRACT_CATEGORY =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-observation-extract-category"
    const val SDC_OBSERVATION_CATEGORY = "http://hl7.org/fhir/observation-category"
    const val SDC_CATEGORY_SURVEY = "survey"
    const val SDC_QUESTIONNAIRE_LAUNCH_CONTEXT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-launchContext"
    const val SDC_QUESTIONNAIRE_ADAPTIVE =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-questionnaireAdaptive"

    const val SDC_QUESTIONNAIRE_SUB_QUESTIONNAIRE =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-subQuestionnaire"
    const val SDC_QUESTIONNAIRE_CALCULATED_EXPRESSION =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression"

    // $apply parameter names
    const val APPLY_PARAMETER_ACTIVITY_DEFINITION = "activityDefinition"
    const val APPLY_PARAMETER_PLAN_DEFINITION = "planDefinition"
    const val APPLY_PARAMETER_CANONICAL = "canonical"
    const val APPLY_PARAMETER_SUBJECT = "subject"
    const val APPLY_PARAMETER_ENCOUNTER = "encounter"
    const val APPLY_PARAMETER_PRACTITIONER = "practitioner"
    const val APPLY_PARAMETER_ORGANIZATION = "organization"
    const val APPLY_PARAMETER_USER_TYPE = "userType"
    const val APPLY_PARAMETER_USER_LANGUAGE = "userLanguage"
    const val APPLY_PARAMETER_USER_TASK_CONTEXT = "userTaskContext"
    const val APPLY_PARAMETER_SETTING = "setting"
    const val APPLY_PARAMETER_SETTING_CONTEXT = "settingContext"
    const val APPLY_PARAMETER_PARAMETERS = "parameters"
    const val APPLY_PARAMETER_USE_SERVER_DATA = "useServerData"
    const val APPLY_PARAMETER_DATA = "data"
    const val APPLY_PARAMETER_DATA_ENDPOINT = "dataEndpoint"
    const val APPLY_PARAMETER_CONTENT_ENDPOINT = "contentEndpoint"
    const val APPLY_PARAMETER_TERMINOLOGY_ENDPOINT = "terminologyEndpoint"

    const val US_PH_CONTEXT_URL = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context"
    const val LIBRARY_TYPE = "http://terminology.hl7.org/CodeSystem/library-type"
    const val ASSET_COLLECTION = "asset-collection"

    /* CRMI V1 Manifest Parameter Names */
    const val SYSTEM_VERSION = "system-version" // For CodeSystems
    const val CANONICAL_VERSION = "canonicalVersion"

    /* CRMI V2 Manifest Parameter Names */
    const val DEFAULT_SYSTEM_VERSION = "default-system-version"
    const val DEFAULT_CANONICAL_VERSION = "default-canonical-version"
    const val DEFAULT_VALUESET_VERSION = "default-valueset-version"

    const val AUTHORITATIVE_SOURCE_URL =
        "http://hl7.org/fhir/StructureDefinition/valueset-authoritativeSource"

    const val VSAC_BASE_URL = "https://cts.nlm.nih.gov/fhir"
    const val VSAC_USERNAME = "vsacUsername"
    const val APIKEY = "apiKey"
    const val STATUS_ACTIVE = "active"
    const val STATUS_DRAFT = "draft"
    const val RELATEDARTIFACT_TYPE_DEPENDSON = "depends-on"
    const val RESOURCETYPE_VALUESET = "ValueSet"
    const val RESOURCETYPE_CODESYSTEM = "CodeSystem"

    /**
     * The `tx-resource` parameter of the `$expand` operation (R6 OperationDefinition). Additional
     * value sets or code systems referred to from the value set being expanded, used preferentially
     * to those known to the terminology server.
     */
    const val TX_RESOURCE = "tx-resource"

    const val EMPTY = "empty"

    const val PREVIOUS_VALUE = "previousValue"

    enum class CPG_ACTIVITY_TYPE_CODE(code: String) {
        SEND_MESSAGE("send-message"),
        COLLECT_INFORMATION("collect-information");

        @JvmField val code: String?

        init {
            this.code = code
        }
    }

    enum class CqfApplicabilityBehavior {
        ALL,
        ANY,
    }

    enum class SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE {
        PATIENT,
        ENCOUNTER,
        LOCATION,
        USER,
        STUDY,
        CLINICAL,
    }
}
