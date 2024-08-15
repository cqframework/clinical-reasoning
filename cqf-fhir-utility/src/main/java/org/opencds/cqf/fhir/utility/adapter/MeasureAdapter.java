package org.opencds.cqf.fhir.utility.adapter;

import java.util.Arrays;
import java.util.List;
import org.opencds.cqf.fhir.utility.Constants;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
public interface MeasureAdapter {
    public static List<String> CANONICAL_EXTENSIONS =
            Arrays.asList(Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS, Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS);

    public static List<String> REFERENCE_EXTENSIONS = Arrays.asList(
            Constants.CQFM_INPUT_PARAMETERS, Constants.CQF_EXPANSION_PARAMETERS, Constants.CQF_CQL_OPTIONS);
}
