package org.opencds.cqf.fhir.cr.hapi.config.test;

import ca.uhn.fhir.batch2.jobs.bulkmodify.reindex.ReindexProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerCacheRefresher;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerCacheFactory;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerCacheRefresherImpl;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerRegistryImpl;
import ca.uhn.fhir.jpa.cache.ResourceChangeListenerRegistryInterceptor;
import ca.uhn.fhir.jpa.graphql.GraphQLProvider;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.provider.DiffProvider;
import ca.uhn.fhir.jpa.provider.IJpaSystemProvider;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.provider.ValueSetOperationProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryResourceMatcher;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.rest.server.IncomingRequestAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.hapi.common.CodeCacheResourceChangeListener;
import org.opencds.cqf.fhir.cr.hapi.common.ElmCacheResourceChangeListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Common hapi-fhir clinical reasoning config shared with downstream modules.
 */
@Configuration
@Import({SubscriptionSubmitterConfig.class, SubscriptionChannelConfig.class})
public class TestCrConfig {
    public static final String LIBRARY_RESOURCE_NAME = "Library";
    public static final String VALUESET_RESOURCE_NAME = "ValueSet";

    @Bean
    public RestfulServer restfulServer(
            IFhirSystemDao<?, ?> fhirSystemDao,
            DaoRegistry daoRegistry,
            IJpaSystemProvider jpaSystemProvider,
            ResourceProviderFactory resourceProviderFactory,
            ISearchParamRegistry searchParamRegistry,
            IValidationSupport validationSupport,
            DatabaseBackedPagingProvider databaseBackedPagingProvider,
            ValueSetOperationProvider valueSetOperationProvider,
            ReindexProvider reindexProvider,
            ApplicationContext appCtx) {
        RestfulServer ourRestServer = new RestfulServer(fhirSystemDao.getContext());

        TerminologyUploaderProvider terminologyUploaderProvider = appCtx.getBean(TerminologyUploaderProvider.class);

        ourRestServer.registerProviders(resourceProviderFactory.createProviders());
        ourRestServer.registerProvider(jpaSystemProvider);
        ourRestServer.registerProviders(terminologyUploaderProvider, reindexProvider);
        ourRestServer.registerProvider(appCtx.getBean(GraphQLProvider.class));
        ourRestServer.registerProvider(appCtx.getBean(DiffProvider.class));
        ourRestServer.registerProvider(appCtx.getBean(ValueSetOperationProvider.class));
        ourRestServer.setServerAddressStrategy(new IncomingRequestAddressStrategy());

        return ourRestServer;
    }

    @Bean
    public TestCqlProperties testCqlProperties() {
        return new TestCqlProperties();
    }

    @Bean
    public JpaStorageSettings storageSettings() {
        // Storage Settings for CR unit tests are set up in TestCrStorageSettingsConfigurer
        // so that they can be reset for each test invocation
        return new JpaStorageSettings();
    }

    @Bean
    public TestCrStorageSettingsConfigurer storageSettingsConfigurer(JpaStorageSettings storageSettings) {
        return new TestCrStorageSettingsConfigurer(storageSettings);
    }

    @Bean
    public ModelManager modelManager(Map<ModelIdentifier, Model> globalModelCache) {
        return new ModelManager(globalModelCache);
    }

    @Bean
    public Map<VersionedIdentifier, CompiledLibrary> globalLibraryCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<ModelIdentifier, Model> globalModelCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Map<String, List<Code>> globalValueSetCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ElmCacheResourceChangeListener elmCacheResourceChangeListener(
            IResourceChangeListenerRegistry resourceChangeListenerRegistry,
            PartitionSettings partitionSettings,
            DaoRegistry daoRegistry,
            EvaluationSettings evaluationSettings) {
        ElmCacheResourceChangeListener listener =
                new ElmCacheResourceChangeListener(daoRegistry, evaluationSettings.getLibraryCache());
        registerResourceResourceChangeListener(
                resourceChangeListenerRegistry, partitionSettings, listener, LIBRARY_RESOURCE_NAME);
        return listener;
    }

    @Bean
    public CodeCacheResourceChangeListener codeCacheResourceChangeListener(
            IResourceChangeListenerRegistry resourceChangeListenerRegistry,
            EvaluationSettings evaluationSettings,
            PartitionSettings partitionSettings,
            DaoRegistry daoRegistry) {

        CodeCacheResourceChangeListener listener =
                new CodeCacheResourceChangeListener(daoRegistry, evaluationSettings.getValueSetCache());
        // registry
        registerResourceResourceChangeListener(
                resourceChangeListenerRegistry, partitionSettings, listener, VALUESET_RESOURCE_NAME);

        return listener;
    }

    @Bean
    public IResourceChangeListenerRegistry resourceChangeListenerRegistry(
            InMemoryResourceMatcher inMemoryResourceMatcher,
            FhirContext fhirContext,
            PartitionSettings partitionSettings,
            ResourceChangeListenerCacheFactory resourceChangeListenerCacheFactory) {
        return new ResourceChangeListenerRegistryImpl(
                fhirContext, partitionSettings, resourceChangeListenerCacheFactory, inMemoryResourceMatcher);
    }

    @Bean
    IResourceChangeListenerCacheRefresher resourceChangeListenerCacheRefresher() {
        return new ResourceChangeListenerCacheRefresherImpl();
    }

    @Bean
    public ResourceChangeListenerRegistryInterceptor resourceChangeListenerRegistryInterceptor() {
        return new ResourceChangeListenerRegistryInterceptor();
    }

    @Bean
    public PartitionSettings partitionSettings() {
        return new PartitionSettings();
    }

    private void registerResourceResourceChangeListener(
            IResourceChangeListenerRegistry resourceChangeListenerRegistry,
            PartitionSettings partitionSettings,
            IResourceChangeListener listener,
            String resourceType) {

        resourceChangeListenerRegistry.registerResourceResourceChangeListener(
                resourceType,
                getRequestPartitionId(partitionSettings),
                SearchParameterMap.newSynchronous(),
                listener,
                1000);
    }

    @Nonnull
    private RequestPartitionId getRequestPartitionId(PartitionSettings partitionSettings) {
        return RequestPartitionId.defaultPartition(partitionSettings);
    }
}
