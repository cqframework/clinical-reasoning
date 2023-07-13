package org.opencds.cqf.cql.evaluator.content_test.opioid_mme_r4.configuration;

import static org.mockito.Mockito.mock;

import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.common.SubjectProvider;
import org.opencds.cqf.cql.evaluator.measure.r4.R4RepositorySubjectProvider;
import org.opencds.cqf.cql.evaluator.spring.EvaluatorConfiguration;
import org.opencds.cqf.fhir.api.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

@Configuration
@Import(EvaluatorConfiguration.class)
public class TestConfigurationR4 {

  @Bean
  FhirContext fhirContext() {
    return FhirContext.forCached(FhirVersionEnum.R4);
  }

  @Bean
  Repository repository() {
    return mock(Repository.class);
  }

  @Bean
  MeasureEvaluationOptions measureEvaluationOptions() {
    return MeasureEvaluationOptions.defaultOptions();
  }

  @Bean
  SubjectProvider subjectProvider() {
    return new R4RepositorySubjectProvider(repository());
  }
}
