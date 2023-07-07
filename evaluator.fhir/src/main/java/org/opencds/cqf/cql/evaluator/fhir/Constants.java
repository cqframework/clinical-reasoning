package org.opencds.cqf.cql.evaluator.fhir;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ca.uhn.fhir.model.api.Tag;

public class Constants {

  private Constants() {}

  public static final String CQL_ENGINE_DEVICE =
      "http://cqframework.org/fhir/Device/clinical-quality-language";

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
  public static final String PERTAINS_TO_GOAL =
      "http://hl7.org/fhir/StructureDefinition/resource-pertainsToGoal";
  public static final String QUESTIONNAIRE_RESPONSE_AUTHOR =
      "http://hl7.org/fhir/StructureDefinition/questionnaireresponse-author";
  public static final String CPG_QUESTIONNAIRE_GENERATE =
      "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-questionnaire-generate";
  // public static final String CQF_QUESTIONNAIRE =
  // "http://hl7.org/fhir/StructureDefinition/cqf-questionnaire";
  public static final String CQF_EXPRESSION =
      "http://hl7.org/fhir/StructureDefinition/cqf-expression";
  // This is only for dstu3 since the Expression type does not exist in that version
  public static final String CQF_EXPRESSION_LANGUAGE =
      "http://hl7.org/fhir/StructureDefinition/cqf-expression-language";
  public static final String CQF_LIBRARY = "http://hl7.org/fhir/StructureDefinition/cqf-library";
  public static final String DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE =
      "http://hl7.org/fhir/us/davinci-dtr/StructureDefinition/dtr-questionnaireresponse-questionnaire";
  public static final String EXT_CRMI_MESSAGES =
      "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-messages";
  public static final String SDC_QUESTIONNAIRE_HIDDEN =
      "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden";
  public static final String SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT =
      "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext";
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
}
