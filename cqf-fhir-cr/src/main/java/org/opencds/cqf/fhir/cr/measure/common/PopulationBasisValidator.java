package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.execution.EvaluationResult;

/**
 * Validates group populations and stratifiers against population basis-es.
 */
public interface PopulationBasisValidator {

    void validateGroupPopulations(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult);

    void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult);
}
