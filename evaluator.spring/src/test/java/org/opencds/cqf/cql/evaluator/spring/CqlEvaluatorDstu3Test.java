package org.opencds.cqf.cql.evaluator.spring;

import static org.testng.Assert.assertNotNull;

import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.measure.dstu3.Dstu3MeasureProcessor;
import org.opencds.cqf.cql.evaluator.spring.configuration.TestConfigurationDstu3;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration(classes = TestConfigurationDstu3.class)
public class CqlEvaluatorDstu3Test extends AbstractTestNGSpringContextTests {

    @Test
    public void canInstantiateMeasureProcessor() {
        Dstu3MeasureProcessor measureProcessor = this.applicationContext.getBean(Dstu3MeasureProcessor.class);
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