package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.opencds.cqf.fhir.cr.measure.common.CqlEvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;

/**
 * Population basis validator for DSTU3 (STU3).
 * <p/>
 * The DSTU3 Measure resource does not support the {@code cqfm-populationBasis} extension —
 * all measures are implicitly patient-based (boolean). Stratifiers in DSTU3 are flat elements
 * with only {@code criteria} (a CQL expression name) or {@code path} (a FHIR resource path),
 * with no component sub-elements and no criteria-vs-value distinction. Because there is no
 * population basis concept in the DSTU3 spec, there is nothing to validate.
 */
public class Dstu3PopulationBasisValidator implements PopulationBasisValidator {

    /**
     * No-op. DSTU3 measures have no population basis extension, so group population
     * results cannot mismatch a basis that does not exist.
     */
    @Override
    public void validateGroupPopulations(
            MeasureDef measureDef, GroupDef groupDef, CqlEvaluationResult evaluationResult) {
        // no-op
    }

    /**
     * No-op. DSTU3 stratifiers are simple expressions with no return-type constraints
     * tied to a population basis. The criteria-vs-value stratifier classification and
     * the allowed-types validation are R4+ (CQFMeasures IG) concepts.
     */
    @Override
    public void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, CqlEvaluationResult evaluationResult) {
        // no-op
    }
}
