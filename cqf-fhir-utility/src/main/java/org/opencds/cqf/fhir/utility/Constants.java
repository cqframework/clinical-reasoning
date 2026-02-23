package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.model.api.Tag;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class Constants {

    private Constants() {}

    public static final String CQL_ENGINE_DEVICE = "http://cqframework.org/fhir/Device/clinical-quality-language";

    public static final String HL7_FHIR_REST = "hl7-fhir-rest";
    public static final String HL7_FHIR_FILES = "hl7-fhir-files";
    public static final String HL7_CQL_FILES = "hl7-cql-files";

    public static final Tag HL7_FHIR_REST_CODE = new Tag(null, HL7_FHIR_REST);
    public static final Tag HL7_FHIR_FILES_CODE = new Tag(null, HL7_FHIR_FILES);
    public static final Tag HL7_CQL_FILES_CODE = new Tag(null, HL7_CQL_FILES);

    public static final String FHIR_MODEL_URI = "http://hl7.org/fhir";
    public static final String QDM_MODEL_URI = "urn:healthit-gov:qdm:v5_4";

    public static final Map<String, String> ALIAS_MAP =
            ImmutableMap.of("FHIR", FHIR_MODEL_URI, "QUICK", FHIR_MODEL_URI, "QDM", QDM_MODEL_URI);

    public static final String ALT_EXPRESSION_EXT =
            "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-alternativeExpression";

    public static final String FHIR_TYPE_EXTENSION =
            "http://hl7.org/fhir/StructureDefinition/structuredefinition-fhir-type";
    public static final String DISPLAY_EXTENSION = "http://hl7.org/fhir/StructureDefinition/display";
    public static final String PERTAINS_TO_GOAL = "http://hl7.org/fhir/StructureDefinition/resource-pertainsToGoal";
    public static final String REQUEST_DO_NOT_PERFORM = "http://hl7.org/fhir/StructureDefinition/request-doNotPerform";
    public static final String QUESTIONNAIRE_RESPONSE_AUTHOR =
            "http://hl7.org/fhir/StructureDefinition/questionnaireresponse-author";
    public static final String QUESTIONNAIRE_REFERENCE_PROFILE =
            "http://hl7.org/fhir/StructureDefinition/questionnaire-referenceProfile";
    public static final String QUESTIONNAIRE_UNIT = "http://hl7.org/fhir/StructureDefinition/questionnaire-unit";
    public static final String QUESTIONNAIRE_UNIT_VALUE_SET =
            "http://hl7.org/fhir/StructureDefinition/questionnaire-unitValueSet";
    public static final String VARIABLE_EXTENSION = "http://hl7.org/fhir/StructureDefinition/variable";

    public static final String CPG_ASSERTION_EXPRESSION =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-assertionExpression";
    public static final String CPG_FEATURE_EXPRESSION =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-featureExpression";
    public static final String CPG_FEATURE_EXPRESSION_ELEMENT =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-featureExpressionElement";
    public static final String CPG_INFERENCE_EXPRESSION =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-inferenceExpression";
    public static final String CPG_KNOWLEDGE_CAPABILITY =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-knowledgeCapability";
    public static final String CPG_KNOWLEDGE_REPRESENTATION_LEVEL =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-knowledgeRepresentationLevel";
    public static final String CPG_QUESTIONNAIRE_GENERATE =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-questionnaire-generate";
    public static final String CPG_RATIONALE = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-rationale";
    public static final String CPG_RELATED_ARTIFACT =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-relatedArtifact";
    public static final String ARTIFACT_RELATED_ARTIFACT =
            "http://hl7.org/fhir/StructureDefinition/artifact-relatedArtifact";
    public static final String CPG_SERVICE_REQUEST =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-servicerequest";
    public static final String CPG_STRATEGY = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-strategy";
    public static final String CPG_INPUT_TEXT = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-input-text";
    public static final String CPG_INPUT_DESCRIPTION =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-input-description";
    public static final String CPG_PARAMETER_DEFINITION =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition";

    public static final String CPG_CUSTOM_ACTIVITY_KIND =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-custom-activity-kind";
    public static final String CPG_ACTIVITY_KIND = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-activity-kind";
    public static final String CPG_ACTIVITY_TYPE_CS = "http://hl7.org/fhir/uv/cpg/CodeSystem/cpg-activity-type-cs";

    public enum CPG_ACTIVITY_TYPE_CODE {
        SEND_MESSAGE("send-message"),
        COLLECT_INFORMATION("collect-information");

        public final String code;

        CPG_ACTIVITY_TYPE_CODE(String code) {
            this.code = code;
        }
    }

    public static final String CPG_SUMMARY_FOR = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-summaryFor";
    public static final String CPG_GENERATED_FOR = "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-generatedFor";

    public static final String CPG_CASE_PLAN_SUMMARY =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-caseplansummary";
    public static final String CPG_RELATED_SUMMARY_DEFINITION =
            "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-relatedsummarydefinition";

    public static final String CQF_RESOURCETYPE = "http://hl7.org/fhir/StructureDefinition/cqf-resourceType";

    // DSTU3 CQF Extensions
    public static final String CQIF_LIBRARY = "http://hl7.org/fhir/StructureDefinition/cqif-library";
    public static final String CQIF_CQL_EXPRESSION = "http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression";

    public static final String CQF_APPLICABILITY_BEHAVIOR =
            "http://hl7.org/fhir/StructureDefinition/cqf-applicabilityBehavior";

    public enum CqfApplicabilityBehavior {
        ALL,
        ANY
    }

    public static final String ARTIFACT_IS_OWNED_EXTENSION_URL =
            "http://hl7.org/fhir/StructureDefinition/artifact-isOwned";
    public static final String CQF_CQL_OPTIONS = "http://hl7.org/fhir/StructureDefinition/cqf-cqlOptions";
    public static final String CQF_EXPANSION_PARAMETERS =
            "http://hl7.org/fhir/StructureDefinition/cqf-expansionParameters";
    public static final String CQF_INPUT_EXPANSION_PARAMETERS =
            "http://hl7.org/fhir/StructureDefinition/cqf-inputParameters";
    public static final String CQF_EXPRESSION = "http://hl7.org/fhir/StructureDefinition/cqf-expression";
    public static final String CQF_LIBRARY = "http://hl7.org/fhir/StructureDefinition/cqf-library";
    public static final String CQF_CALCULATED_VALUE = "http://hl7.org/fhir/StructureDefinition/cqf-calculatedValue";
    public static final String CQF_FHIR_QUERY_PATTERN = "http://hl7.org/fhir/StructureDefinition/cqf-fhirQueryPattern";
    public static final String CQF_DIRECT_REFERENCE_EXTENSION =
            "http://hl7.org/fhir/StructureDefinition/cqf-directReferenceCode";
    public static final String CQF_MESSAGES = "http://hl7.org/fhir/StructureDefinition/cqf-messages";
    public static final String CQF_LOGIC_DEFINITION = "http://hl7.org/fhir/StructureDefinition/cqf-logicDefinition";

    public static final String CQFM_EFFECTIVE_DATA_REQUIREMENTS =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-effectiveDataRequirements";
    public static final String CQFM_LOGIC_DEFINITION =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-logicDefinition";
    public static final String CQFM_SOFTWARE_SYSTEM =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-softwaresystem";
    public static final String CQFM_INPUT_PARAMETERS =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-inputParameters";
    public static final String CQFM_COMPONENT = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-component";
    public static final String CQFM_DIRECT_REFERENCE_EXTENSION =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-directReferenceCode";
    public static final String CRMI_EFFECTIVE_DATA_REQUIREMENTS =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-effectiveDataRequirements";
    public static final String CRMI_DEPENDENCY_ROLE =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-dependencyRole";
    public static final String CRMI_REFERENCE_SOURCE =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-referenceSource";
    public static final String PACKAGE_SOURCE = "http://hl7.org/fhir/StructureDefinition/package-source";
    public static final String CRMI_VERSION_1 = "1.0.0";
    public static final String CRMI_VERSION_2 = "2.0.0";

    public static final String CRMI_OPERATION_APPROVE = "$approve";
    public static final String CRMI_OPERATION_ARTIFACT_DIFF = "$artifact-diff";
    public static final String CRMI_OPERATION_DATA_REQUIREMENTS = "$data-requirements";
    public static final String CRMI_OPERATION_DELETE = "$delete";
    public static final String CRMI_OPERATION_PACKAGE = "$package";
    public static final String CRMI_OPERATION_RELEASE = "$release";
    public static final String CRMI_OPERATION_RETIRE = "$retire";
    public static final String CRMI_OPERATION_REVISE = "$revise";
    public static final String CRMI_OPERATION_WITHDRAW = "$withdraw";

    public static final String DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE =
            "http://hl7.org/fhir/us/davinci-dtr/StructureDefinition/dtr-questionnaireresponse-questionnaire";
    public static final String SDC_QUESTIONNAIRE_HIDDEN =
            "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden";
    public static final String SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext";
    public static final String SDC_QUESTIONNAIRE_DEFINITION_EXTRACT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtract";
    public static final String SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtractValue";
    public static final String SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemPopulationContext";
    public static final String SDC_QUESTIONNAIRE_IS_SUBJECT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-isSubject";
    public static final String SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-prepopulate-subject";
    public static final String SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaireresponse-isSubject";
    public static final String SDC_QUESTIONNAIRE_INITIAL_EXPRESSION =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression";
    public static final String SDC_QUESTIONNAIRE_CANDIDATE_EXPRESSION =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-candidateExpression";
    public static final String SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-lookupQuestionnaire";
    public static final String SDC_QUESTIONNAIRE_PREPOPULATE =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-prepopulate";
    public static final String SDC_QUESTIONNAIRE_PREPOPULATE_PARAMETER =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-prepopulate-parameter";
    public static final String SDC_QUESTIONNAIRE_SHORT_TEXT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-shortText";
    public static final String SDC_QUESTIONNAIRE_OBSERVATION_EXTRACT_CATEGORY =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-observation-extract-category";
    public static final String SDC_OBSERVATION_CATEGORY = "http://hl7.org/fhir/observation-category";
    public static final String SDC_CATEGORY_SURVEY = "survey";
    public static final String SDC_QUESTIONNAIRE_LAUNCH_CONTEXT =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-launchContext";
    public static final String SDC_QUESTIONNAIRE_ADAPTIVE =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-questionnaireAdaptive";

    public enum SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE {
        PATIENT,
        ENCOUNTER,
        LOCATION,
        USER,
        STUDY,
        CLINICAL
    }

    public static final String SDC_QUESTIONNAIRE_SUB_QUESTIONNAIRE =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-subQuestionnaire";
    public static final String SDC_QUESTIONNAIRE_CALCULATED_EXPRESSION =
            "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression";

    // $apply parameter names
    public static final String APPLY_PARAMETER_ACTIVITY_DEFINITION = "activityDefinition";
    public static final String APPLY_PARAMETER_PLAN_DEFINITION = "planDefinition";
    public static final String APPLY_PARAMETER_CANONICAL = "canonical";
    public static final String APPLY_PARAMETER_SUBJECT = "subject";
    public static final String APPLY_PARAMETER_ENCOUNTER = "encounter";
    public static final String APPLY_PARAMETER_PRACTITIONER = "practitioner";
    public static final String APPLY_PARAMETER_ORGANIZATION = "organization";
    public static final String APPLY_PARAMETER_USER_TYPE = "userType";
    public static final String APPLY_PARAMETER_USER_LANGUAGE = "userLanguage";
    public static final String APPLY_PARAMETER_USER_TASK_CONTEXT = "userTaskContext";
    public static final String APPLY_PARAMETER_SETTING = "setting";
    public static final String APPLY_PARAMETER_SETTING_CONTEXT = "settingContext";
    public static final String APPLY_PARAMETER_PARAMETERS = "parameters";
    public static final String APPLY_PARAMETER_USE_SERVER_DATA = "useServerData";
    public static final String APPLY_PARAMETER_DATA = "data";
    public static final String APPLY_PARAMETER_DATA_ENDPOINT = "dataEndpoint";
    public static final String APPLY_PARAMETER_CONTENT_ENDPOINT = "contentEndpoint";
    public static final String APPLY_PARAMETER_TERMINOLOGY_ENDPOINT = "terminologyEndpoint";

    public static final String US_PH_CONTEXT_URL = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";
    public static final String LIBRARY_TYPE = "http://terminology.hl7.org/CodeSystem/library-type";
    public static final String ASSET_COLLECTION = "asset-collection";
    /* CRMI V1 Manifest Parameter Names */
    public static final String SYSTEM_VERSION = "system-version"; // For CodeSystems
    public static final String CANONICAL_VERSION = "canonicalVersion";
    /* CRMI V2 Manifest Parameter Names */
    public static final String DEFAULT_SYSTEM_VERSION = "default-system-version";
    public static final String DEFAULT_CANONICAL_VERSION = "default-canonical-version";
    public static final String DEFAULT_VALUESET_VERSION = "default-valueset-version";

    public static final String AUTHORITATIVE_SOURCE_URL =
            "http://hl7.org/fhir/StructureDefinition/valueset-authoritativeSource";

    public static final String VSAC_USERNAME = "vsacUsername";
    public static final String APIKEY = "apiKey";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DRAFT = "draft";
    public static final String RELATEDARTIFACT_TYPE_DEPENDSON = "depends-on";
    public static final String RESOURCETYPE_VALUESET = "ValueSet";
    public static final String RESOURCETYPE_CODESYSTEM = "CodeSystem";

    public static final String EMPTY = "empty";

    public static final String PREVIOUS_VALUE = "previousValue";
}
