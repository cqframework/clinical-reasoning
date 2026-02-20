package org.opencds.cqf.fhir.cql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleBuilder;
import jakarta.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.StreamSupport;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmPackageManager;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.cqframework.fhir.utilities.LoggerAdapter;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.data.SystemDataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.engine.retrieve.FederatedDataProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EnginesTest {

    private static final String URN_HL_7_ORG_ELM_TYPES_R_1 = "urn:hl7-org:elm-types:r1";

    private static final Logger log = LoggerFactory.getLogger(EnginesTest.class);

    private InMemoryFhirRepository repository;

    @BeforeEach
    void beforeEach() {
        repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repository.update(new Patient().setId("pat1"));
    }

    @Test
    void defaultSettings() {
        var engine = Engines.forRepository(repository);

        assertDataProviders(engine);
    }

    @Test
    void modelCacheNull() {
        var settings = EvaluationSettings.getDefault().withModelCache(null);
        assertNull(settings.getModelCache());
        var engine = getEngine(settings);

        assertDataProviders(engine);
    }

    @Test
    void duplicateSettingsNpmProcessor() {
        var settings = EvaluationSettings.getDefault()
                .withModelCache(new HashMap<>())
                .withValueSetCache(new HashMap<>())
                .withNpmProcessor(mock(NpmProcessor.class))
                .withLibraryCache(new HashMap<>())
                .withLibrarySourceProviders(List.of(new StringLibrarySourceProvider(new ArrayList<>())))
                .withTerminologySettings(new TerminologySettings())
                .withCqlOptions(new CqlOptions().setUseEmbeddedLibraries(false))
                .withRetrieveSettings(new RetrieveSettings());

        settings.getCqlOptions().setUseEmbeddedLibraries(false);
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(true);

        var copiedSettings = new EvaluationSettings(settings);

        assertThat(copiedSettings.getLibrarySourceProviders(), hasSize(1));
        assertTrue(copiedSettings.getModelCache().isEmpty());
        assertTrue(copiedSettings.getValueSetCache().isEmpty());
        assertTrue(copiedSettings.getLibraryCache().isEmpty());
        assertThat(copiedSettings.getLibrarySourceProviders(), hasSize(1));
        assertFalse(copiedSettings.getCqlOptions().useEmbeddedLibraries());
        assertNotNull(copiedSettings.getNpmProcessor());

        var engine = getEngine(settings);

        assertTrue(engine.getState().getDebugMap().isLoggingEnabled());

        assertDataProviders(engine);
    }

    @Test
    void duplicateSettingsNullNpmProcessor() {
        var settings = EvaluationSettings.getDefault();
        settings.setModelCache(new HashMap<>());
        settings.setValueSetCache(new HashMap<>());
        settings.getCqlOptions().setUseEmbeddedLibraries(false);
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(true);
        settings.getLibrarySourceProviders().add(new StringLibrarySourceProvider(new ArrayList<>()));
        settings.setNpmProcessor(null);

        var copiedSettings = new EvaluationSettings(settings);

        assertThat(copiedSettings.getLibrarySourceProviders(), hasSize(1));
        assertTrue(copiedSettings.getModelCache().isEmpty());
        assertTrue(copiedSettings.getValueSetCache().isEmpty());
        assertThat(copiedSettings.getLibrarySourceProviders(), hasSize(1));
        assertFalse(copiedSettings.getCqlOptions().useEmbeddedLibraries());
        assertNull(copiedSettings.getNpmProcessor());

        var engine = getEngine(settings);

        assertTrue(engine.getState().getDebugMap().isLoggingEnabled());

        assertDataProviders(engine);
    }

    @Test
    void emptyLibrarySourceProviders() {
        var settings = EvaluationSettings.getDefault();
        settings.setLibrarySourceProviders(List.of());
        assertTrue(settings.getLibrarySourceProviders().isEmpty());
        var engine = getEngine(settings);

        assertDataProviders(engine);
    }

    @Test
    void useEmbeddedLibrariesFalse() {
        var settings = EvaluationSettings.getDefault();
        settings.getCqlOptions().setUseEmbeddedLibraries(false);
        assertFalse(settings.getCqlOptions().useEmbeddedLibraries());
        var engine = getEngine(settings);

        assertDataProviders(engine);
    }

    @Test
    void debugLoggingEnabled() {
        var settings = EvaluationSettings.getDefault();
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(true);
        var engine = getEngine(settings);
        assertTrue(engine.getState().getDebugMap().isLoggingEnabled());

        assertDataProviders(engine);
    }

    @Test
    void debugLoggingDisabled() {
        var settings = EvaluationSettings.getDefault();
        settings.getCqlOptions().getCqlEngineOptions().setDebugLoggingEnabled(false);
        var engine = getEngine(settings);
        assertNull(engine.getState().getDebugMap());

        assertDataProviders(engine);
    }

    @Test
    void librarySourceProviders() {
        var settings = EvaluationSettings.getDefault();
        settings.getLibrarySourceProviders().add(new StringLibrarySourceProvider(List.of()));
        assertThat(settings.getLibrarySourceProviders(), hasSize(1));
        var engine = getEngine(settings);
        assertNull(engine.getState().getDebugMap());

        assertDataProviders(engine);
    }

    @Test
    void npmProcessorNull() {
        var settings = EvaluationSettings.getDefault().withNpmProcessor(null);

        var engine = getEngine(settings);
        var lm = engine.getEnvironment().getLibraryManager();

        var ni = lm.getNamespaceManager().getNamespaceInfoFromUri("http://fhir.org/guides/cqf/common");
        assertNull(ni);

        assertDataProviders(engine);
    }

    @Test
    void npmProcessorIgContextNull() {
        var npmProcessor = new NpmProcessor(null);
        var settings = EvaluationSettings.getDefault().withNpmProcessor(npmProcessor);

        var engine = getEngine(settings);
        var lm = engine.getEnvironment().getLibraryManager();

        var ni = lm.getNamespaceManager().getNamespaceInfoFromUri("http://fhir.org/guides/cqf/common");
        assertNull(ni);

        assertDataProviders(engine);
    }

    @Test
    void npmProcessorPackageManagerNull() {
        var npmProcessor = mock(NpmProcessor.class);
        when(npmProcessor.getPackageManager()).thenReturn(null);
        when(npmProcessor.getIgContext()).thenReturn(mock(IGContext.class));
        when(npmProcessor.getPackageManager()).thenReturn(null);

        var settings = EvaluationSettings.getDefault().withNpmProcessor(npmProcessor);

        var engine = getEngine(settings);
        var lm = engine.getEnvironment().getLibraryManager();

        var ni = lm.getNamespaceManager().getNamespaceInfoFromUri("http://fhir.org/guides/cqf/common");
        assertNull(ni);

        assertDataProviders(engine);
    }

    @Test
    void npmProcessorWithDupeNamespacesById() {
        var npmProcessor = mock(NpmProcessor.class);
        var namespaceInfo1 = new NamespaceInfo("1", "a");
        var namespaceInfo2 = new NamespaceInfo("1", "b");
        when(npmProcessor.getNamespaces()).thenReturn(List.of(namespaceInfo1, namespaceInfo2));
        when(npmProcessor.getIgContext()).thenReturn(mock(IGContext.class));
        // Mockito does not respect @Nonnull annotation, so we have to mock this to avoid NPEs
        when(npmProcessor.getIgContext().getFhirVersion())
                .thenReturn(repository.fhirContext().getVersion().getVersion().toString());
        when(npmProcessor.getPackageManager()).thenReturn(mock(NpmPackageManager.class));
        var settings = EvaluationSettings.getDefault().withNpmProcessor(npmProcessor);

        var engine = getEngine(settings);
        var lm = engine.getEnvironment().getLibraryManager();

        var ni = lm.getNamespaceManager().getNamespaceInfoFromUri("http://fhir.org/guides/cqf/common");
        assertNull(ni);

        assertDataProviders(engine);
    }

    @Test
    void npmProcessorWithDupeNamespacesByUrl() {
        var npmProcessor = mock(NpmProcessor.class);
        var namespaceInfo1 = new NamespaceInfo("1", "a");
        var namespaceInfo2 = new NamespaceInfo("2", "a");
        when(npmProcessor.getNamespaces()).thenReturn(List.of(namespaceInfo1, namespaceInfo2));
        when(npmProcessor.getIgContext()).thenReturn(mock(IGContext.class));
        // Mockito does not respect @Nonnull annotation, so we have to mock this to avoid NPEs
        when(npmProcessor.getIgContext().getFhirVersion())
                .thenReturn(repository.fhirContext().getVersion().getVersion().toString());
        when(npmProcessor.getPackageManager()).thenReturn(mock(NpmPackageManager.class));
        var settings = EvaluationSettings.getDefault().withNpmProcessor(npmProcessor);

        var engine = getEngine(settings);
        var lm = engine.getEnvironment().getLibraryManager();

        var ni = lm.getNamespaceManager().getNamespaceInfoFromUri("http://fhir.org/guides/cqf/common");
        assertNull(ni);

        assertDataProviders(engine);
    }

    @Test
    void npmProcessor() {
        var resourceDirectory = org.opencds.cqf.fhir.test.Resources.getResourcePath(getClass());
        var ini = Path.of(resourceDirectory).resolve("org/opencds/cqf/fhir/cql/npm/ig.ini");

        var igContext = new IGContext(new LoggerAdapter(log));
        igContext.initializeFromIni(ini.toString());
        var settings = EvaluationSettings.getDefault().withNpmProcessor(new NpmProcessor(igContext));

        var engine = getEngine(settings);
        var lm = engine.getEnvironment().getLibraryManager();

        var ni = lm.getNamespaceManager().getNamespaceInfoFromUri("http://fhir.org/guides/cqf/common");
        assertNotNull(ni);
        assertEquals("fhir.cqf.common", ni.getName());

        assertDataProviders(engine);
    }

    @Test
    void additionalDataNull() {
        var settings = EvaluationSettings.getDefault();
        var engine = getEngine(settings, null);
        assertNull(engine.getState().getDebugMap());

        assertDataProviders(engine);
    }

    @Test
    void additionalDataEmpty() {
        var settings = EvaluationSettings.getDefault();
        var engine = getEngine(settings, new BundleBuilder(FhirContext.forR4Cached()).getBundle());
        assertNull(engine.getState().getDebugMap());

        assertDataProviders(engine);
    }

    @Test
    void additionalDataEntry() {
        var settings = EvaluationSettings.getDefault();

        var bundleBuilder = new BundleBuilder(FhirContext.forR4Cached());
        bundleBuilder.addTransactionCreateEntry(new Encounter().setId("en1"));
        var additionalData = bundleBuilder.getBundle();

        var engine = Engines.forRepository(repository, settings, additionalData);

        assertNotNull(engine.getState());

        var dataProviders = engine.getEnvironment().getDataProviders();

        assertNotNull(dataProviders);
        assertEquals(2, dataProviders.size());

        assertThat(dataProviders.keySet(), containsInAnyOrder(Constants.FHIR_MODEL_URI, URN_HL_7_ORG_ELM_TYPES_R_1));

        var dataProvider1 = dataProviders.get(Constants.FHIR_MODEL_URI);
        var dataProvider2 = dataProviders.get(URN_HL_7_ORG_ELM_TYPES_R_1);

        assertInstanceOf(FederatedDataProvider.class, dataProvider1);
        assertInstanceOf(SystemDataProvider.class, dataProvider2);

        var federatedDataProvider = (FederatedDataProvider) dataProvider1;

        var retrievedPatients = retrieve(Patient.class, federatedDataProvider);
        assertThat(retrievedPatients, hasSize(1));

        var retrieveEncounters = retrieve(Encounter.class, federatedDataProvider);
        assertThat(retrieveEncounters, hasSize(1));
    }

    @Test
    void getCqlFhirParametersConverter() {
        assertNotNull(Engines.getCqlFhirParametersConverter(FhirContext.forR4Cached()));
    }

    private static void assertDataProviders(CqlEngine engine) {
        var dataProviders = engine.getEnvironment().getDataProviders();

        assertNotNull(dataProviders);
        assertEquals(2, dataProviders.size());

        assertThat(dataProviders.keySet(), containsInAnyOrder(Constants.FHIR_MODEL_URI, URN_HL_7_ORG_ELM_TYPES_R_1));

        var dataProvider1 = dataProviders.get(Constants.FHIR_MODEL_URI);
        var dataProvider2 = dataProviders.get(URN_HL_7_ORG_ELM_TYPES_R_1);

        assertInstanceOf(FederatedDataProvider.class, dataProvider1);
        assertInstanceOf(SystemDataProvider.class, dataProvider2);

        var federatedDataProvider = (FederatedDataProvider) dataProvider1;

        var retrievedPatients = retrieve(Patient.class, federatedDataProvider);

        assertThat(retrievedPatients, hasSize(1));
    }

    private static class NpmProcessorWithDupeNamespaces extends NpmProcessor {

        public NpmProcessorWithDupeNamespaces(IGContext igContext) {
            super(igContext);
        }

        @Override
        public List<NamespaceInfo> getNamespaces() {
            final NamespaceInfo namespaceInfo = new NamespaceInfo("1", "1");
            return List.of(namespaceInfo, namespaceInfo);
        }
    }

    private static <T extends Resource> List<T> retrieve(Class<T> clazz, FederatedDataProvider provider) {
        return convert(
                clazz,
                provider.retrieve(
                        null, null, null, clazz.getSimpleName(), null, null, null, null, null, null, null, null));
    }

    private static <T extends Resource> List<T> convert(Class<T> clazz, Iterable<Object> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
    }

    @Nonnull
    private CqlEngine getEngine(EvaluationSettings settings) {
        return Engines.forRepository(repository, settings);
    }

    @Nonnull
    private CqlEngine getEngine(EvaluationSettings settings, IBaseBundle bundle) {
        return Engines.forRepository(repository, settings, bundle);
    }
}
