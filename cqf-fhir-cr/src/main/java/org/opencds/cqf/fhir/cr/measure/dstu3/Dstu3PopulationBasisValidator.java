package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;

public class Dstu3PopulationBasisValidator implements PopulationBasisValidator {

    @Override
    public void validateGroupPopulationBasisType(String url, GroupDef groupDef, ExpressionResult expressionResult) {

        // TODO: LD: Implement this if there's ever a requirement to validate DSTU3 Population Basis
    }

    @Override
    public void validateStratifierPopulationBasisType(
            String url, GroupDef groupDef, ExpressionResult expressionResult) {

        // TODO: LD: Implement this if there's ever a requirement to validate DSTU3 Population Basis
    }
}
