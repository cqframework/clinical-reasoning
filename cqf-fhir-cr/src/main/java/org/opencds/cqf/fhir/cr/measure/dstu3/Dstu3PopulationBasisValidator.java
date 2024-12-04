package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;

public class Dstu3PopulationBasisValidator implements PopulationBasisValidator {

    @Override
    public void validateGroupPopulations(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
        // TODO: LD: Implement this if there's ever a requirement to validate DSTU3 Population Basis
    }

    @Override
    public void validateStratifiers(MeasureDef measureDef, GroupDef theGroupDef, EvaluationResult theEvaluationResult) {
        // TODO: LD: Implement this if there's ever a requirement to validate DSTU3 Population Basis
    }
}
