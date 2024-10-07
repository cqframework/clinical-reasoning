package org.opencds.cqf.fhir.utility.adapter;

import java.util.Arrays;
import java.util.List;
import org.opencds.cqf.fhir.utility.Constants;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
public interface QuestionnaireAdapter extends KnowledgeArtifactAdapter {
    public static List<String> REFERENCE_EXTENSIONS = Arrays.asList(
            Constants.QUESTIONNAIRE_UNIT_VALUE_SET,
            Constants.QUESTIONNAIRE_REFERENCE_PROFILE,
            Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE,
            Constants.SDC_QUESTIONNAIRE_SUB_QUESTIONNAIRE);

    public static List<String> EXPRESSION_EXTENSIONS = Arrays.asList(
            Constants.VARIABLE_EXTENSION,
            Constants.SDC_QUESTIONNAIRE_CANDIDATE_EXPRESSION,
            Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION,
            Constants.SDC_QUESTIONNAIRE_CALCULATED_EXPRESSION,
            Constants.CQF_EXPRESSION);
}
