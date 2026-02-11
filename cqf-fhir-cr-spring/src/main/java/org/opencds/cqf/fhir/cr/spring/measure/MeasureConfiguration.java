package org.opencds.cqf.fhir.cr.spring.measure;

import ca.uhn.fhir.repository.IRepository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.opencds.cqf.fhir.cr.measure")
public class MeasureConfiguration {

    @Bean
    Dstu3MeasureProcessor dstu3MeasureProcessor(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            SubjectProvider subjectProvider) {
        return new Dstu3MeasureProcessor(repository, measureEvaluationOptions, subjectProvider);
    }

    @Bean
    R4MeasureProcessor r4MeasureProcessor(IRepository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        return new R4MeasureProcessor(repository, measureEvaluationOptions);
    }
}
