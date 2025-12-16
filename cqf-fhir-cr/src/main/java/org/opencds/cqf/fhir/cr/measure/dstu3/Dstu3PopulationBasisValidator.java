package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;
import org.opencds.cqf.fhir.cr.measure.common.def.report.GroupReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

/**
 * Validates group populations and stratifiers against population basis-es for DSTU3.
 * <p/>
 * Currently no-ops.
 */
public class Dstu3PopulationBasisValidator implements PopulationBasisValidator {

    @Override
    public void validateGroupPopulations(
            MeasureReportDef measureDef, GroupReportDef groupDef, EvaluationResult evaluationResult) {
        // TODO: LD: Implement this if there's ever a requirement to validate DSTU3 Population Basis
    }

    @Override
    public void validateStratifiers(
            MeasureReportDef measureDef, GroupReportDef groupDef, EvaluationResult evaluationResult) {
        // TODO: LD: Implement this if there's ever a requirement to validate DSTU3 Population Basis
    }
}
