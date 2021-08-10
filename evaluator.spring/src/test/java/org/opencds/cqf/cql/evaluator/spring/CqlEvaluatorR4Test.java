package org.opencds.cqf.cql.evaluator.spring;

import static org.testng.Assert.assertNotNull;

import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.cql.evaluator.spring.configuration.TestConfigurationR4;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration(classes = TestConfigurationR4.class)
public class CqlEvaluatorR4Test extends AbstractTestNGSpringContextTests {

    @Test
    public void canInstantiateMeasureProcessor() {
        R4MeasureProcessor measureProcessor = this.applicationContext.getBean(R4MeasureProcessor.class);
        assertNotNull(measureProcessor);
    }

    @Test
    public void canInstantiateLibraryProcessor() {
        LibraryProcessor libraryProcessor = this.applicationContext.getBean(LibraryProcessor.class);
        assertNotNull(libraryProcessor);
    }

    @Test
    public void canInstantiateExpressionEvaluator() {
        ExpressionEvaluator expressionEvaluator = this.applicationContext.getBean(ExpressionEvaluator.class);
        assertNotNull(expressionEvaluator);
    }
}