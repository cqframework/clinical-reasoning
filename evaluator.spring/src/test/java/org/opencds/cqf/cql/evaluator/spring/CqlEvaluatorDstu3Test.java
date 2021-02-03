package org.opencds.cqf.cql.evaluator.spring;

import static org.testng.Assert.assertNotNull;

import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.spring.configuration.TestConfigurationDstu3;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration(classes = TestConfigurationDstu3.class)
public class CqlEvaluatorDstu3Test extends AbstractTestNGSpringContextTests {

    @Test
    public void canInstantiateDstu3() {
        LibraryProcessor libraryProcessor = this.applicationContext.getBean(LibraryProcessor.class);
        assertNotNull(libraryProcessor);
    }
}