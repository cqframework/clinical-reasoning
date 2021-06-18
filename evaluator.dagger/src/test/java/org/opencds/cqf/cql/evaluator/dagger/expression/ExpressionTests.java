package org.opencds.cqf.cql.evaluator.dagger.expression;

import org.opencds.cqf.cql.evaluator.dagger.DaggerCqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

import static org.testng.Assert.assertNotNull;

public class ExpressionTests {

    @Test
    public void canInstantiateDstu3() {
        ExpressionEvaluator expressionEvaluator = DaggerCqlEvaluatorComponent.builder()
                .fhirContext(FhirContext.forCached(FhirVersionEnum.DSTU3)).build().createExpressionEvaluator();

        assertNotNull(expressionEvaluator);
    }

    @Test
    public void canInstantiateR4() {
        ExpressionEvaluator expressionEvaluator = DaggerCqlEvaluatorComponent.builder()
                .fhirContext(FhirContext.forCached(FhirVersionEnum.R4)).build().createExpressionEvaluator();

        assertNotNull(expressionEvaluator);
    }

}
