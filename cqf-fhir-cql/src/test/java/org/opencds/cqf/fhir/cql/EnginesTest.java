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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.BundleBuilder;
import jakarta.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmPackageManager;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.cqframework.fhir.utilities.LoggerAdapter;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.data.SystemDataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cql.engine.retrieve.FederatedDataProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
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
        var resourceDirectory = ResourceDirectoryResolver.getResourceDirectory();
        var ini = resourceDirectory.resolve("org/opencds/cqf/fhir/cql/npm/ig.ini");

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

    /**
     * All created resources must have an SP that identifies
     * a field that *does not have* a date value between 2000-01-01 and 2000-12-31 eod
     * (ie, in the year 2000).
     */
    static List<Arguments> failureParameters() {
        // formatter
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<Arguments> args = new ArrayList<>();

        // 1 Encounter with period entirely after daterange
        {
            Encounter encounter = new Encounter();
            encounter
                    .addLocation()
                    .setLocation(new Reference("Location/123"))
                    .setPeriod(new Period()
                            .setStart(createDate(formatter, "2001-08-06"))
                            .setEnd(createDate(formatter, "2002-08-05")));

            args.add(Arguments.of(encounter, "location-period"));
        }
        // 2 Encounter with period entirely before daterange
        {
            Encounter encounter = new Encounter();
            encounter
                    .addLocation()
                    .setLocation(new Reference("Location/123"))
                    .setPeriod(new Period()
                            .setStart(createDate(formatter, "1998-08-06"))
                            .setEnd(createDate(formatter, "1999-08-05")));

            args.add(Arguments.of(encounter, "location-period"));
        }
        // 3 Patient with birthday before
        {
            Patient patient = new Patient();
            patient.setActive(true);
            patient.setBirthDate(createDate(formatter, "1999-03-14"));

            args.add(Arguments.of(patient, "birthdate"));
        }
        // 4 Patient with birthday after
        {
            Patient patient = new Patient();
            patient.setActive(true);
            patient.setBirthDate(createDate(formatter, "2001-03-14"));

            args.add(Arguments.of(patient, "birthdate"));
        }
        // 5 Observation with valueDateTime before range
        {
            Observation obs = new Observation();
            obs.setValue(new DateTimeType().setValue(createDate(dateTimeFormatter, "1999-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            args.add(Arguments.of(obs, "value-date"));
        }
        // 6 Observation with valueDateTime after range
        {
            Observation obs = new Observation();
            obs.setValue(new DateTimeType().setValue(createDate(dateTimeFormatter, "2001-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            args.add(Arguments.of(obs, "value-date"));
        }

        return args;
    }

    /**
     * All created resources must have a SP that identifies
     * a field that has a date value between 2000-01-01 and 2000-12-31 eod
     * (ie, in the year 2000).
     */
    static List<Arguments> successfulParameters() {
        // formatter
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<Arguments> args = new ArrayList<>();

        // 1 Encounter with location period starting in 2000
        {
            Encounter encounter = new Encounter();
            encounter
                    .addLocation()
                    .setLocation(new Reference("Location/123"))
                    .setPeriod(new Period()
                            .setStart(createDate(formatter, "2000-11-11"))
                            .setEnd(createDate(formatter, "2001-02-28")));

            args.add(Arguments.of(encounter, "location-period"));
        }
        // 2 Encounter with location period ending in 2000
        {
            Encounter encounter = new Encounter();
            encounter
                    .addLocation()
                    .setLocation(new Reference("Location/123"))
                    .setPeriod(new Period()
                            .setStart(createDate(formatter, "1999-08-06"))
                            .setEnd(createDate(formatter, "2000-04-01")));

            args.add(Arguments.of(encounter, "location-period"));
        }
        // 3 Encounter with location period around all of period
        {
            Encounter encounter = new Encounter();
            encounter
                    .addLocation()
                    .setLocation(new Reference("Location/123"))
                    .setPeriod(new Period()
                            .setStart(createDate(formatter, "1999-08-06"))
                            .setEnd(createDate(formatter, "2001-08-05")));

            args.add(Arguments.of(encounter, "location-period"));
        }
        //  Patient with birthDate in period
        {
            Patient patient = new Patient();
            patient.setActive(true);
            patient.setBirthDate(createDate(formatter, "2000-08-13"));

            args.add(Arguments.of(patient, "birthdate"));
        }
        //  Observation with effective datetime value in 2000
        {
            Observation obs = new Observation();
            obs.setValue(new DateTimeType().setValue(createDate(dateTimeFormatter, "2000-03-14 02:59:00")));
            obs.setStatus(ObservationStatus.CORRECTED);

            args.add(Arguments.of(obs, "value-date"));
        }

        return args;
    }

    @ParameterizedTest
    @MethodSource("successfulParameters")
    public void retrieve_withValidDatesInRange_succeeds(IBaseResource resource, String spName) {
        // test
        var results = retrieveResourcesWithin2000BySPName(resource, spName);

        // verify
        assertNotNull(results);
        List<Object> resources = new ArrayList<>();
        results.forEach(resources::add);
        assertEquals(1, resources.size());
    }

    @ParameterizedTest
    @MethodSource("failureParameters")
    public void retrieve_withValidDatesOutOfRange_failToRetrieve(IBaseResource resource, String spName) {
        // setup
        IParser parser = repository.fhirContext().newJsonParser();

        // test
        var results = retrieveResourcesWithin2000BySPName(resource, spName);

        // verify
        assertNotNull(results);
        List<Object> resources = new ArrayList<>();
        results.forEach(resources::add);
        assertTrue(
                resources.isEmpty(),
                String.join(
                        "\n",
                        resources.stream()
                                .map(r -> (IBaseResource) r)
                                .map(parser::encodeToString)
                                .collect(Collectors.toSet())));
    }

    private Iterable<Object> retrieveResourcesWithin2000BySPName(IBaseResource resource, String spName) {
        // setup
        String resourceType = resource.fhirType();

        RetrieveSettings retrieveSettings = new RetrieveSettings();
        retrieveSettings.setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY);

        EvaluationSettings settings = EvaluationSettings.getDefault().withRetrieveSettings(retrieveSettings);

        var engine = Engines.forRepository(repository, settings);
        var dataProviders = engine.getEnvironment().getDataProviders();
        var dataProvider = (FederatedDataProvider) dataProviders.get(Constants.FHIR_MODEL_URI);

        repository.update(resource, Map.of());

        // test
        var start = new DateTime("2000-01-01T00:00:00", ZoneOffset.UTC);
        var end = new DateTime("2000-12-31T23:59:59", ZoneOffset.UTC);
        var dateRange = new Interval(start, true, end, true);

        // Retrieve resources with period overlapping 2000
        var results = dataProvider.retrieve(
                resourceType, // context
                null, // contextPath
                "pat1", // contextValue
                resourceType, // dataType
                null, // templateId
                null, // codePath
                null, // codes
                null, // valueSet
                spName, // datePath - but it's actually the name of the sp
                "period.start", // dateLowPath
                "period.end", // dateHighPath
                dateRange // dateRange
                );

        return results;
    }

    @Test
    public void dateFiltering() {
        // setup
        RetrieveSettings retrieveSettings = new RetrieveSettings();
        retrieveSettings.setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY);

        EvaluationSettings settings = EvaluationSettings.getDefault().withRetrieveSettings(retrieveSettings);

        var engine = Engines.forRepository(repository, settings);
        var dataProviders = engine.getEnvironment().getDataProviders();
        var dataProvider = (FederatedDataProvider) dataProviders.get(Constants.FHIR_MODEL_URI);

        Patient patient = new Patient();
        patient.setId("Patient/123");
        patient.setBirthDate(createDate(new SimpleDateFormat("MM/dd/yyyy"), "12/13/2024"));
        repository.update(patient, Map.of());

        // test
        // TODO - do ranges and exact values?
        var start = new DateTime("2024-01-01T00:00:00", ZoneOffset.UTC);
        var end = new DateTime("2024-12-31T23:59:59", ZoneOffset.UTC);
        var dateRange = new Interval(start, true, end, true);

        // Retrieve encounters with period overlapping 2024
        var results = dataProvider.retrieve(
                "Patient", // context
                null, // "subject",      // contextPath
                "pat1", // contextValue
                "Patient", // dataType
                null, // templateId
                null, // codePath
                null, // codes
                null, // valueSet
                "birthdate", // datePath
                "period.start", // dateLowPath
                "period.end", // dateHighPath
                dateRange // dateRange
                );

        // verify
        assertNotNull(results);
        List<Object> resources = new ArrayList<>();
        results.forEach(resources::add);
        assertEquals(1, resources.size());
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

    private static Date createDate(SimpleDateFormat formatter, String dateStr) {
        try {
            return formatter.parse(dateStr);
        } catch (ParseException ex) {
            fail(ex);
            return null;
        }
    }
}
