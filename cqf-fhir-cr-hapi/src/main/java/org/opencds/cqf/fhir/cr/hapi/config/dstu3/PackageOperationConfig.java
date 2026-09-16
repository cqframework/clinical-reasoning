package org.opencds.cqf.fhir.cr.hapi.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.AsyncPackageOperationHelper;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.PackageJobService;
import org.opencds.cqf.fhir.cr.hapi.common.PackageJobStatusProvider;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.dstu3.library.LibraryPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.plandefinition.PlanDefinitionPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.questionnaire.QuestionnairePackageProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.valueset.ValueSetPackageProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class PackageOperationConfig {
    @Bean
    PlanDefinitionPackageProvider dstu3PlanDefinitionPackageProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory,
            AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new PlanDefinitionPackageProvider(planDefinitionProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean
    QuestionnairePackageProvider dstu3QuestionnairePackageProvider(
            IQuestionnaireProcessorFactory questionnaireProcessorFactory,
            AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new QuestionnairePackageProvider(questionnaireProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean
    LibraryPackageProvider dstu3LibraryPackageProvider(
            ILibraryProcessorFactory libraryProcessorFactory, AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new LibraryPackageProvider(libraryProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean
    ValueSetPackageProvider dstu3ValueSetPackageProvider(
            IValueSetProcessorFactory valueSetProcessorFactory,
            AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new ValueSetPackageProvider(valueSetProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean(name = "packageOperationLoader")
    public ProviderLoader packageOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.DSTU3,
                        Arrays.asList(
                                LibraryPackageProvider.class,
                                QuestionnairePackageProvider.class,
                                PlanDefinitionPackageProvider.class,
                                ValueSetPackageProvider.class,
                                PackageJobStatusProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }

    @Bean
    PackageJobService packageJobService() {
        return new PackageJobService();
    }

    @Bean
    AsyncPackageOperationHelper asyncPackageOperationHelper(PackageJobService packageJobService) {
        return new AsyncPackageOperationHelper(packageJobService);
    }

    @Bean
    PackageJobStatusProvider packageJobStatusProvider(PackageJobService packageJobService) {
        return new PackageJobStatusProvider(packageJobService);
    }
}
