package org.opencds.cqf.fhir.cr.spring.measure;

import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.npm.R4NpmPackageLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.opencds.cqf.fhir.cr.measure")
public class MeasureConfiguration {

    @Bean
    Dstu3MeasureProcessor dstu3MeasureProcessor(
            Repository repository, MeasureEvaluationOptions measureEvaluationOptions, SubjectProvider subjectProvider) {
        return new Dstu3MeasureProcessor(repository, measureEvaluationOptions, subjectProvider);
    }

    @Bean
    R4MeasureProcessor r4MeasureProcessor(
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            SubjectProvider subjectProvider,
            R4MeasureServiceUtils measureServiceUtils,
            R4NpmPackageLoader r4NpmPackageLoader) {
        return new R4MeasureProcessor(
                repository, measureEvaluationOptions, subjectProvider, measureServiceUtils, r4NpmPackageLoader);
    }
}
