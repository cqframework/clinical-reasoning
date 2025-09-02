package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.opencds.cqf.fhir.cql.Engines.EngineInitializationContext;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.cpg.r4.R4CqlExecutionService;
import org.opencds.cqf.fhir.cr.crmi.R4ApproveService;
import org.opencds.cqf.fhir.cr.crmi.R4DraftService;
import org.opencds.cqf.fhir.cr.crmi.R4ReleaseService;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.hapi.config.CrBaseConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.config.RepositoryConfig;
import org.opencds.cqf.fhir.cr.hapi.r4.IApproveServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.ICareGapsServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.ICollectDataServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.ICqlExecutionServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.IDataRequirementsServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.IDraftServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.IReleaseServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.ISubmitDataProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureEvaluatorMultipleFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureEvaluatorSingleFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureServiceUtilsFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.cpg.CqlExecutionOperationProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.crmi.ApproveProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.crmi.DraftProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.crmi.ReleaseProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.measure.CareGapsOperationProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.measure.CollectDataOperationProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.measure.DataRequirementsOperationProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.measure.MeasureOperationsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.measure.SubmitDataProvider;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.R4CareGapsService;
import org.opencds.cqf.fhir.cr.measure.r4.R4CollectDataService;
import org.opencds.cqf.fhir.cr.measure.r4.R4DataRequirementsService;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureService;
import org.opencds.cqf.fhir.cr.measure.r4.R4MultiMeasureService;
import org.opencds.cqf.fhir.cr.measure.r4.R4SubmitDataService;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.npm.NpmConfigDependencySubstitutor;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RepositoryConfig.class, CrBaseConfig.class, ReleaseOperationConfig.class})
public class CrR4Config {

    @Bean
    R4MeasureEvaluatorSingleFactory r4MeasureServiceFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            MeasureEvaluationOptions evaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);
            return new R4MeasureService(
                    repository,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationOptions.getEvaluationSettings()),
                    evaluationOptions,
                    measurePeriodValidator);
        };
    }

    @Bean
    R4MeasureEvaluatorMultipleFactory r4MeasureEvaluatorMultipleFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            MeasureEvaluationOptions evaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);
            return new R4MultiMeasureService(
                    repository,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationOptions.getEvaluationSettings()),
                    evaluationOptions,
                    requestDetails.getFhirServerBase(),
                    measurePeriodValidator);
        };
    }

    @Bean
    ISubmitDataProcessorFactory r4SubmitDataProcessorFactory(IRepositoryFactory repositoryFactory) {
        return requestDetails -> new R4SubmitDataService(repositoryFactory.create(requestDetails));
    }

    @Bean
    ICqlExecutionServiceFactory r4CqlExecutionServiceFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            EvaluationSettings evaluationSettings) {

        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);
            return new R4CqlExecutionService(
                    repository,
                    evaluationSettings,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationSettings));
        };
    }

    @Bean
    CqlExecutionOperationProvider r4CqlExecutionOperationProvider(
            ICqlExecutionServiceFactory cqlExecutionServiceFactory) {
        return new CqlExecutionOperationProvider(cqlExecutionServiceFactory);
    }

    @Bean
    CollectDataOperationProvider r4CollectDataOperationProvider(
            ICollectDataServiceFactory r4CollectDataServiceFactory, StringTimePeriodHandler stringTimePeriodHandler) {
        return new CollectDataOperationProvider(r4CollectDataServiceFactory, stringTimePeriodHandler);
    }

    @Bean
    R4MeasureServiceUtilsFactory r4MeasureServiceUtilsFactory(IRepositoryFactory repositoryFactory) {
        return requestDetails -> new R4MeasureServiceUtils(repositoryFactory.create(requestDetails));
    }

    @Bean
    ICollectDataServiceFactory collectDataServiceFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            MeasureEvaluationOptions measureEvaluationOptions,
            R4MeasureServiceUtilsFactory r4MeasureServiceUtilsFactory) {
        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);
            return new R4CollectDataService(
                    repository,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            measureEvaluationOptions.getEvaluationSettings()),
                    measureEvaluationOptions);
        };
    }

    @Bean
    DataRequirementsOperationProvider r4DataRequirementsOperationProvider(
            IDataRequirementsServiceFactory r4DataRequirementsServiceFactory) {
        return new DataRequirementsOperationProvider(r4DataRequirementsServiceFactory);
    }

    @Bean
    IDataRequirementsServiceFactory dataRequirementsServiceFactory(
            IRepositoryFactory repositoryFactory, MeasureEvaluationOptions measureEvaluationOptions) {
        return rd -> new R4DataRequirementsService(repositoryFactory.create(rd), measureEvaluationOptions);
    }

    @Bean
    ICareGapsServiceFactory careGapsServiceFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            CareGapsProperties careGapsProperties,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);
            return new R4CareGapsService(
                    careGapsProperties,
                    repository,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            measureEvaluationOptions.getEvaluationSettings()),
                    measureEvaluationOptions,
                    requestDetails.getFhirServerBase(),
                    measurePeriodValidator);
        };
    }

    @Bean
    CareGapsOperationProvider r4CareGapsOperationProvider(
            ICareGapsServiceFactory r4CareGapsProcessorFactory, StringTimePeriodHandler stringTimePeriodHandler) {
        return new CareGapsOperationProvider(r4CareGapsProcessorFactory, stringTimePeriodHandler);
    }

    @Bean
    IDraftServiceFactory draftServiceFactory(IRepositoryFactory repositoryFactory) {
        return rd -> new R4DraftService(repositoryFactory.create(rd));
    }

    @Bean
    DraftProvider r4DraftProvider(IDraftServiceFactory r4DraftServiceFactory) {
        return new DraftProvider(r4DraftServiceFactory);
    }

    @Bean
    IApproveServiceFactory approveServiceFactory(IRepositoryFactory repositoryFactory) {
        return rd -> new R4ApproveService(repositoryFactory.create(rd));
    }

    @Bean
    ApproveProvider r4ApproveProvider(IApproveServiceFactory r4ApproveServiceFactory) {
        return new ApproveProvider(r4ApproveServiceFactory);
    }

    @Bean
    IReleaseServiceFactory releaseServiceFactory(IRepositoryFactory repositoryFactory) {
        return rd -> new R4ReleaseService(repositoryFactory.create(rd));
    }

    @Bean
    ReleaseProvider r4ReleaseProvider(IReleaseServiceFactory r4ReleaseServiceFactory) {
        return new ReleaseProvider(r4ReleaseServiceFactory);
    }

    @Bean
    SubmitDataProvider r4SubmitDataProvider(ISubmitDataProcessorFactory r4SubmitDataProcessorFactory) {
        return new SubmitDataProvider(r4SubmitDataProcessorFactory);
    }

    @Bean
    MeasureOperationsProvider r4MeasureOperationsProvider(
            R4MeasureEvaluatorSingleFactory r4MeasureServiceFactory,
            R4MeasureEvaluatorMultipleFactory r4MultiMeasureServiceFactory,
            StringTimePeriodHandler stringTimePeriodHandler) {
        return new MeasureOperationsProvider(
                r4MeasureServiceFactory, r4MultiMeasureServiceFactory, stringTimePeriodHandler);
    }

    @Bean
    public ProviderLoader r4PdLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {

        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
                        Arrays.asList(
                                MeasureOperationsProvider.class,
                                SubmitDataProvider.class,
                                CareGapsOperationProvider.class,
                                CqlExecutionOperationProvider.class,
                                CollectDataOperationProvider.class,
                                DataRequirementsOperationProvider.class,
                                DraftProvider.class,
                                ApproveProvider.class,
                                ReleaseProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}
