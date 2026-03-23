package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.execution.EvaluationResult;

/**
 * Validates group populations and stratifiers against population basis-es.
 */
public interface PopulationBasisValidator {

    /** No-op validator for FHIR versions that don't require population basis validation. */
    PopulationBasisValidator NOOP = new PopulationBasisValidator() {
        @Override
        public void validateGroupPopulations(
                MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
            // intentionally empty
        }

        @Override
        public void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
            // intentionally empty
        }
    };

    void validateGroupPopulations(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult);

    void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult);
}
