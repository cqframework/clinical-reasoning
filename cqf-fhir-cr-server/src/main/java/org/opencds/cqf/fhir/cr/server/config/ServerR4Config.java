package org.opencds.cqf.fhir.cr.server.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureEvaluatorMultipleFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.R4MeasureEvaluatorSingleFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.measure.MeasureOperationsProvider;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.R4MultiMeasureService;
import org.opencds.cqf.fhir.cr.server.RepositoryRestProviderRegistrar;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * R4 server wiring. Provides every bean needed to stand up a clinical reasoning server backed by
 * an in-memory {@link IRepository}: FHIR context, RestfulServer, the operation provider chain,
 * the CRUD shim, and the servlet mount.
 *
 * <p>This config does not {@code @Import} {@code CrR4Config} because that drags in the JPA
 * {@code RepositoryConfig} (requires {@code DaoRegistry}). The relevant beans are recreated here
 * with {@link IRepositoryFactory} returning the in-memory repository instead.
 */
@Configuration
@EnableConfigurationProperties(ServerProperties.class)
public class ServerR4Config {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4Cached();
    }

    @Bean
    public RestfulServer restfulServer(FhirContext fhirContext) {
        return new RestfulServer(fhirContext);
    }

    @Bean
    public ServletRegistrationBean<RestfulServer> restfulServerRegistration(
            RestfulServer restfulServer, ServerProperties properties) {
        var basePath = properties.getBasePath();
        var pattern = basePath.endsWith("/") ? basePath + "*" : basePath + "/*";
        var registration = new ServletRegistrationBean<>(restfulServer, pattern);
        registration.setName("fhirServlet");
        return registration;
    }

    @Bean
    public IRepository inMemoryRepository(FhirContext fhirContext) {
        return new InMemoryFhirRepository(fhirContext);
    }

    @Bean
    public IRepositoryFactory repositoryFactory(IRepository inMemoryRepository) {
        // Single shared in-memory store; ignore RequestDetails (no per-tenant scoping yet).
        return rd -> inMemoryRepository;
    }

    @Bean
    public RepositoryRestProviderRegistrar repositoryProviderRegistrar(
            RestfulServer restfulServer, IRepositoryFactory repositoryFactory, FhirContext fhirContext) {
        // null = register every concrete resource type known to the FhirContext.
        return new RepositoryRestProviderRegistrar(restfulServer, repositoryFactory, fhirContext, null);
    }

    // -------------------- CR operation chain --------------------
    // Mirrors the bean shape of CrR4Config but without that class's @Import on the JPA
    // RepositoryConfig. When CrR4Config is split upstream, replace this section with an @Import
    // of the operation-only config.

    @Bean
    public EvaluationSettings evaluationSettings() {
        return EvaluationSettings.getDefault();
    }

    @Bean
    public TerminologyServerClientSettings terminologyServerClientSettings() {
        return TerminologyServerClientSettings.getDefault();
    }

    @Bean
    public CrSettings crSettings(
            EvaluationSettings evaluationSettings, TerminologyServerClientSettings terminologySettings) {
        return new CrSettings()
                .withEvaluationSettings(evaluationSettings)
                .withTerminologyServerClientSettings(terminologySettings);
    }

    @Bean
    public MeasureEvaluationOptions measureEvaluationOptions() {
        return MeasureEvaluationOptions.defaultOptions();
    }

    @Bean
    public MeasurePeriodValidator measurePeriodValidator() {
        return new MeasurePeriodValidator();
    }

    @Bean
    public StringTimePeriodHandler stringTimePeriodHandler() {
        return new StringTimePeriodHandler(ZoneOffset.UTC);
    }

    @Bean
    public R4MeasureEvaluatorSingleFactory r4MeasureEvaluatorSingleFactory(
            IRepositoryFactory repositoryFactory,
            MeasureEvaluationOptions evaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        return (requestDetails, environment) -> new R4MultiMeasureService(
                environment.resolve(repositoryFactory.create(requestDetails)),
                evaluationOptions,
                requestDetails.getFhirServerBase(),
                measurePeriodValidator);
    }

    @Bean
    public R4MeasureEvaluatorMultipleFactory r4MeasureEvaluatorMultipleFactory(
            IRepositoryFactory repositoryFactory,
            MeasureEvaluationOptions evaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        return (requestDetails, environment) -> new R4MultiMeasureService(
                environment.resolve(repositoryFactory.create(requestDetails)),
                evaluationOptions,
                requestDetails.getFhirServerBase(),
                measurePeriodValidator);
    }

    @Bean
    public MeasureOperationsProvider r4MeasureOperationsProvider(
            R4MeasureEvaluatorSingleFactory single,
            R4MeasureEvaluatorMultipleFactory multi,
            StringTimePeriodHandler timeHandler) {
        return new MeasureOperationsProvider(single, multi, timeHandler);
    }

    @Bean
    public ProviderLoader providerLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector =
                new ProviderSelector(fhirContext, Map.of(FhirVersionEnum.R4, List.of(MeasureOperationsProvider.class)));
        return new ProviderLoader(restfulServer, applicationContext, selector);
    }

    // RestfulServer.init() runs naturally when Tomcat initializes the servlet — at that point
    // ProviderLoader has already registered all providers (it fires on ContextRefreshedEvent,
    // which precedes servlet container startup). Manually pre-warming via @PostConstruct caused
    // double-registration warnings because @PostConstruct runs before ContextRefreshedEvent.
}
