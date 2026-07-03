package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.AsyncPackageOperationHelper;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.PackageJobService;
import org.opencds.cqf.fhir.cr.hapi.common.PackageJobStatusProvider;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r4.graphdefinition.GraphDefinitionPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.plandefinition.PlanDefinitionPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.questionnaire.QuestionnairePackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.valueset.ValueSetPackageProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class PackageOperationConfig {
    @Bean
    PlanDefinitionPackageProvider r4PlanDefinitionPackageProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory,
            AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new PlanDefinitionPackageProvider(planDefinitionProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean
    QuestionnairePackageProvider r4QuestionnairePackageProvider(
            IQuestionnaireProcessorFactory questionnaireProcessorFactory,
            AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new QuestionnairePackageProvider(questionnaireProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean
    LibraryPackageProvider r4LibraryPackageProvider(
            ILibraryProcessorFactory libraryProcessorFactory, AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new LibraryPackageProvider(libraryProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean
    ValueSetPackageProvider r4ValueSetPackageProvider(
            IValueSetProcessorFactory valueSetProcessorFactory,
            AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new ValueSetPackageProvider(valueSetProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean
    GraphDefinitionPackageProvider r4GraphDefinitionPackageProvider(
            IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory,
            AsyncPackageOperationHelper asyncPackageOperationHelper) {
        return new GraphDefinitionPackageProvider(graphDefinitionProcessorFactory, asyncPackageOperationHelper);
    }

    @Bean(name = "packageOperationLoader")
    public ProviderLoader packageOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
                        Arrays.asList(
                                LibraryPackageProvider.class,
                                QuestionnairePackageProvider.class,
                                PlanDefinitionPackageProvider.class,
                                ValueSetPackageProvider.class,
                                GraphDefinitionPackageProvider.class,
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
