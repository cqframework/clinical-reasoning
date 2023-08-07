package org.opencds.cqf.cql.evaluator.spring.measure;

import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.common.SubjectProvider;
import org.opencds.cqf.cql.evaluator.measure.dstu3.Dstu3MeasureProcessor;
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.opencds.cqf.cql.evaluator.measure")
public class MeasureConfiguration {


  @Bean
  Dstu3MeasureProcessor dstu3MeasureProcessor(Repository repository,
      MeasureEvaluationOptions measureEvaluationOptions, SubjectProvider subjectProvider) {
    return new Dstu3MeasureProcessor(repository, measureEvaluationOptions, subjectProvider);
  }


  @Bean
  R4MeasureProcessor r4MeasureProcessor(Repository repository,
      MeasureEvaluationOptions measureEvaluationOptions, SubjectProvider subjectProvider) {
    return new R4MeasureProcessor(repository, measureEvaluationOptions, subjectProvider);
  }

}
