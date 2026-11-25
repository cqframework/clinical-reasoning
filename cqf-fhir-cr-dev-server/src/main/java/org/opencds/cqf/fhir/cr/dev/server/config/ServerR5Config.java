package org.opencds.cqf.fhir.cr.dev.server.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Map;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.dev.server.RepositoryRestProviderRegistrar;
import org.opencds.cqf.fhir.cr.dev.server.annotations.OnR5Condition;
import org.opencds.cqf.fhir.cr.hapi.config.r5.ApplyOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r5.CqlOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r5.EvaluateOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r5.ExtractOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r5.PackageOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r5.PopulateOperationConfig;
import org.opencds.cqf.fhir.cr.hapi.config.r5.QuestionnaireOperationConfig;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * R5 server wiring. Provides every bean needed to stand up a clinical reasoning server backed by
 * an in-memory {@link IRepository}: FHIR context, RestfulServer, the operation provider chain,
 * the CRUD shim, and the servlet mount.
 *
 * <p>This config does not {@code @Import} {@code CrR5Config} because that drags in the JPA
 * {@code RepositoryConfig} (requires {@code DaoRegistry}). The relevant beans are recreated here
 * with {@link IRepositoryFactory} returning the in-memory repository instead.
 */
@SuppressWarnings("UnstableApiUsage")
@Configuration
@Conditional(OnR5Condition.class)
@EnableConfigurationProperties(ServerProperties.class)
@Import({
    ApplyOperationConfig.class,
    CqlOperationConfig.class,
    EvaluateOperationConfig.class,
    ExtractOperationConfig.class,
    PackageOperationConfig.class,
    PopulateOperationConfig.class,
    QuestionnaireOperationConfig.class
})
public class ServerR5Config {

    @Bean
    public FhirContext fhirContext(ServerProperties properties) {
        return FhirContext.forCached(FhirVersionEnum.valueOf(properties.getFhirVersion()));
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
        return EvaluationSettings.getDefault()
                .withRegisteredNamespaces(Map.of(
                        "hl7.fhir.uv.cql",
                        "http://hl7.org/fhir/uv/cql",
                        "hl7.fhir.us.cql",
                        "http://hl7.org/fhir/us/cql"));
    }

    @Bean
    public TerminologyServerClientSettings terminologyServerClientSettings() {
        return TerminologyServerClientSettings.getDefault();
    }

    // RestfulServer.init() runs naturally when Tomcat initializes the servlet — at that point
    // ProviderLoader has already registered all providers (it fires on ContextRefreshedEvent,
    // which precedes servlet container startup). Manually pre-warming via @PostConstruct caused
    // double-registration warnings because @PostConstruct runs before ContextRefreshedEvent.
}
