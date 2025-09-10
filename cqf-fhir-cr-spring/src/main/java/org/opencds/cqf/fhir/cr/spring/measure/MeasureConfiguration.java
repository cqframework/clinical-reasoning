package org.opencds.cqf.fhir.cr.spring.measure;

import ca.uhn.fhir.repository.IRepository;
import java.util.Optional;
import org.opencds.cqf.fhir.cql.Engines.EngineInitializationContext;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.npm.R4RepositoryOrNpmResourceProvider;
import org.opencds.cqf.fhir.utility.npm.NpmConfigDependencySubstitutor;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.opencds.cqf.fhir.cr.measure")
public class MeasureConfiguration {

    @Bean
    Dstu3MeasureProcessor dstu3MeasureProcessor(
            IRepository repository,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            MeasureEvaluationOptions measureEvaluationOptions,
            SubjectProvider subjectProvider) {
        return new Dstu3MeasureProcessor(
                repository,
                new EngineInitializationContext(
                        repository,
                        NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                        measureEvaluationOptions.getEvaluationSettings()),
                measureEvaluationOptions,
                subjectProvider);
    }

    @Bean
    R4MeasureProcessor r4MeasureProcessor(
            IRepository repository,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasureProcessorUtils measureProcessorUtils,
            R4RepositoryOrNpmResourceProvider r4RepositoryOrNpmResourceProvider) {
        return new R4MeasureProcessor(
                repository,
                new EngineInitializationContext(
                        repository,
                        NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                        measureEvaluationOptions.getEvaluationSettings()),
                measureEvaluationOptions,
                measureProcessorUtils,
                r4RepositoryOrNpmResourceProvider);
    }
}
