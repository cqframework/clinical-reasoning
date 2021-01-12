package org.opencds.cqf.cql.evaluator.evaluation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.testng.annotations.Test;


public class ExpressionEvaluatorTest {

    @Test
    public void test_evaluator_returns_expressionValue() {
        Observation ob = new Observation();
        IntegerType value = new IntegerType(5);
        ob.setValue(value);
        Object result = ExpressionEvaluator.evaluateExpression(ob, "Observation.value");
        assertNotNull(result);
        assertEquals(value, result);
    }
       
}