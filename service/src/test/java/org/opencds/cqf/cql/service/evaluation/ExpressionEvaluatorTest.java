package org.opencds.cqf.cql.service.evaluation;

import org.hl7.fhir.r4.model.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


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