package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.def.report.GroupReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

/**
 * Validates group populations and stratifiers against population basis-es.
 */
public interface PopulationBasisValidator {

    void validateGroupPopulations(
            MeasureReportDef measureDef, GroupReportDef groupDef, EvaluationResult evaluationResult);

    void validateStratifiers(MeasureReportDef measureDef, GroupReportDef groupDef, EvaluationResult evaluationResult);
}
