package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.execution.ExpressionResult;

// LUKETODO:  javadoc
public interface PopulationBasisValidator {

    // LUKETODO:  javadoc
    void validateGroupPopulationBasisType(String url, GroupDef groupDef, ExpressionResult expressionResult);

    // LUKETODO:  javadoc
    void validateStratifierPopulationBasisType(String url, GroupDef groupDef, ExpressionResult expressionResult);
}
