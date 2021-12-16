package org.opencds.cqf.cql.evaluator.spring;

import org.opencds.cqf.cql.evaluator.spring.builder.BuilderConfiguration;
import org.opencds.cqf.cql.evaluator.spring.cql2elm.Cql2ElmConfiguration;
import org.opencds.cqf.cql.evaluator.spring.expression.ExpressionConfiguration;
import org.opencds.cqf.cql.evaluator.spring.fhir.FhirConfiguration;
import org.opencds.cqf.cql.evaluator.spring.library.LibraryConfiguration;
import org.opencds.cqf.cql.evaluator.spring.measure.MeasureConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ExpressionConfiguration.class, LibraryConfiguration.class, Cql2ElmConfiguration.class, FhirConfiguration.class, BuilderConfiguration.class, MeasureConfiguration.class})
public class EvaluatorConfiguration {
    
}
