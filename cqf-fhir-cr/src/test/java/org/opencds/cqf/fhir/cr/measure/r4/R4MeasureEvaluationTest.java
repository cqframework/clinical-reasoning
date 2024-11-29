package org.opencds.cqf.fhir.cr.measure.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import jakarta.annotation.Nullable;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Environment;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.BaseMeasureEvaluationTest;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class R4MeasureEvaluationTest extends BaseMeasureEvaluationTest {

    private static final CodeType POPULATION_BASIS_BOOLEAN = new CodeType("boolean");
    private static final CodeType POPULATION_BASIS_ENCOUNTER = new CodeType("Encounter");

    public String getFhirVersion() {
        return "4.0.1";
    }

    private Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(this.getClass()) + "/org/opencds/cqf/fhir/cr/measure/r4/FHIR347/"));
    private MeasureEvaluationOptions evaluationOptions = MeasureEvaluationOptions.defaultOptions();

    @Test
    void cohortMeasureEvaluation() {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(
                        eq("Patient"),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(Arrays.asList(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n";

        System.out.println("cql = \n" + cql);

        Measure measure = cohort_measure();

        MeasureReport report =
                runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider, null);
        checkEvidence(report);
    }

    @Test
    void sdeInMeasureEvaluation() {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(
                        eq("Patient"),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(Arrays.asList(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n";

        System.out.println("cql = \n" + cql);

        Measure measure = cohort_measure();

        MeasureReport report =
                runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider, null);
        checkEvidence(report);
    }

    @Test
    void proportionMeasureEvaluation() {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(
                        eq("Patient"),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(List.of(patient));

        String cql = cql_with_dateTime() + sde_race()
                + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define Denominator: 'John' in Patient.name.given\n"
                + "define Numerator: Patient.birthDate > @1970-01-01\n";

        System.out.println("cql = \n" + cql);

        Measure measure = proportion_measure();

        MeasureReport report =
                runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider, null);
        checkEvidence(report);
    }

    @Test
    void proportionMeasureEvaluationWithDate() {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(
                        eq("Patient"),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(List.of(patient));

        String cql = cql_with_date() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define Denominator: 'John' in Patient.name.given\n"
                + "define Numerator: AgeInYearsAt(start of \"Measurement Period\") > 18\n";

        System.out.println("cql = \n" + cql);

        Measure measure = proportion_measure();

        MeasureReport report =
                runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider, null);
        checkEvidence(report);
    }

    @Test
    void continuousVariableMeasureEvaluation() {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(
                        eq("Patient"),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(List.of(patient));

        String cql = cql_with_dateTime() + sde_race()
                + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define MeasurePopulation: Patient.birthDate > @1970-01-01\n";

        System.out.println("cql = \n" + cql);

        Measure measure = continuous_variable_measure();

        printResource(measure);

        MeasureReport report =
                runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider, null);
        checkEvidence(report);
    }

    // LUKETODO: cleanup:
    @Language("JSON")
    final String json =
        """
            {
              "resourceType": "Measure",
              "id": "proportion",
              "url": "http://test.com/fhir/Measure/Test",
              "version": "1.0.0",
              "name": "Test",
              "scoring": {
                "coding": [ {
                  "code": "proportion"
                } ]
              },
              "group": [ {
                "population": [ {
                  "id": "initial-population",
                  "code": {
                    "coding": [ {
                      "code": "initial-population"
                    } ]
                  },
                  "criteria": {
                    "expression": "InitialPopulation"
                  }
                }, {
                  "id": "denominator",
                  "code": {
                    "coding": [ {
                      "code": "denominator"
                    } ]
                  },
                  "criteria": {
                    "expression": "Denominator"
                  }
                }, {
                  "id": "numerator",
                  "code": {
                    "coding": [ {
                      "code": "numerator"
                    } ]
                  },
                  "criteria": {
                    "expression": "Numerator"
                  }
                } ],
                "stratifier": [ {
                  "id": "patient-gender",
                  "criteria": {
                    "expression": "Gender"
                  }
                } ]
              } ],
              "supplementalData": [ {
                "id": "sde-race",
                "code": {
                  "text": "sde-race"
                },
                "usage": [ {
                  "coding": [ {
                    "system": "http://terminology.hl7.org/CodeSystem/measure-data-usage",
                    "code": "supplemental-data"
                  } ]
                } ],
                "criteria": {
                  "language": "text/cql",
                  "expression": "SDE Race"
                }
              } ]
            }
        """;

    // Prove we no longer error out for a SUBJECT report with multiple SDEs
    @ParameterizedTest
    @NullSource
    @EnumSource(
            value = MeasureEvalType.class,
            names = {"SUBJECT", "SUBJECTLIST", "POPULATION"})
    void stratifiedMeasureEvaluation(@Nullable MeasureEvalType measureEvalTypeOverride) {

        final String cql = cql_with_dateTime() + sde_race()
                + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define Denominator: 'John' in Patient.name.given\n"
                + "define Numerator: Patient.birthDate > @1970-01-01\n" + "define Gender: Patient.gender\n";

        System.out.println("cql = \n" + cql);

        final MeasureReport report = runTest(
                cql,
                Arrays.asList(jane_doe().getId(), john_doe().getId()),
                stratified_measure(),
            setupMockRetrieverProvider(),
                measureEvalTypeOverride);
        checkStratification(report);
    }

    private static Stream<Arguments> stratifiedMeasureEvaluationByPopulationBasisHappyPathParams() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(POPULATION_BASIS_BOOLEAN, POPULATION_BASIS_BOOLEAN),
            Arguments.of(POPULATION_BASIS_BOOLEAN, POPULATION_BASIS_ENCOUNTER)
        );
    }

    @ParameterizedTest
    @MethodSource("stratifiedMeasureEvaluationByPopulationBasisHappyPathParams")
    void stratifiedMeasureEvaluationByPopulationHappyPathBasis(@Nullable CodeType populationBasisTypeForMeasure, @Nullable CodeType populationBasisTypeForGroup) {
        final String cql = cql_with_dateTime() + sde_race()
            + "define InitialPopulation: 'Doe' in Patient.name.family\n"
            + "define Denominator: 'John' in Patient.name.given\n"
            + "define Numerator: Patient.birthDate > @1970-01-01\n" + "define Gender: Patient.gender\n";

        System.out.println("cql = \n" + cql);

        final MeasureReport report = runTest(
            cql,
            Arrays.asList(jane_doe().getId(), john_doe().getId()),
            stratified_measure(populationBasisTypeForMeasure, populationBasisTypeForGroup),
            setupMockRetrieverProvider(),
            null);
        checkStratification(report);
    }

    private static Stream<Arguments> stratifiedMeasureEvaluationByPopulationBasisErrorPathParams() {
        return Stream.of(
            Arguments.of(POPULATION_BASIS_ENCOUNTER, POPULATION_BASIS_BOOLEAN),
            Arguments.of(POPULATION_BASIS_ENCOUNTER, POPULATION_BASIS_ENCOUNTER)
        );
    }

    // LUKETODO:  can I just shove "SDE Race" into the Numerator expression and call it a day?
    @ParameterizedTest
    @MethodSource("stratifiedMeasureEvaluationByPopulationBasisErrorPathParams")
    void stratifiedMeasureEvaluationByPopulationErrorPathBasis(@Nullable CodeType populationBasisTypeForMeasure, @Nullable CodeType populationBasisTypeForGroup) {
        final String cql = cql_with_dateTime() + sde_race()
            + "define InitialPopulation: 'Doe' in Patient.name.family\n"
            + "define Denominator: 'John' in Patient.name.given\n"
            + "define Numerator: Patient.birthDate > @1970-01-01\n" + "define Gender: Patient.gender\n";

        System.out.println("cql = \n" + cql);

        var x = """
        library Test version '1.0.0'
        
        using FHIR version '4.0.1'
        include FHIRHelpers version '4.0.1'
        
        parameter "Measurement Period" Interval<DateTime> default Interval[@2019-01-01T00:00:00.0, @2020-01-01T00:00:00.0)
        
        context Patient
        define "SDE Race":
          (flatten (
            Patient.extension Extension
              where Extension.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'
                return Extension.extension
          )) E
            where E.url = 'ombCategory'
              or E.url = 'detailed'
            return E.value as Coding
        
        define InitialPopulation: 'Doe' in Patient.name.family
        define Denominator: 'John' in Patient.name.given
        define Numerator: Patient.birthDate > @1970-01-01
        """;

        try {
            runTest(
                cql,
                Arrays.asList(jane_doe().getId(), john_doe().getId()),
                stratified_measure(populationBasisTypeForMeasure, populationBasisTypeForGroup),
                setupMockRetrieverProvider(),
                null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage(), equalTo("stratifier expression criteria results must match the same type as population."));
        }
    }

    @Test
    void evaluatePopulationCriteriaNullResult() {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(
                        eq("Patient"),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(List.of(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: null\n"
                + "define Denominator: null\n" + "define Numerator: null\n";

        Measure measure = proportion_measure();

        MeasureReport report =
                runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider, null);
        checkEvidence(report);
    }

    private void checkStratification(MeasureReport report) {
        MeasureReportGroupStratifierComponent mrgsc = report.getGroupFirstRep().getStratifierFirstRep();
        assertEquals("patient-gender", mrgsc.getId());
        assertEquals(2, mrgsc.getStratum().size());

        StratifierGroupComponent sgc = mrgsc.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().getText().equals("male"))
                .findFirst()
                .orElseThrow();
        StratifierGroupPopulationComponent sgpc = sgc.getPopulation().stream()
                .filter(x -> x.getCode()
                        .getCodingFirstRep()
                        .getCode()
                        .equals(MeasurePopulationType.INITIALPOPULATION.toCode()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, sgpc.getCount());

        sgc = mrgsc.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().getText().equals("female"))
                .findFirst()
                .get();
        sgpc = sgc.getPopulation().stream()
                .filter(x -> x.getCode()
                        .getCodingFirstRep()
                        .getCode()
                        .equals(MeasurePopulationType.INITIALPOPULATION.toCode()))
                .findFirst()
                .get();

        assertEquals(1, sgpc.getCount());
    }

    private MeasureReport runTest(
            String cql,
            List<String> subjectIds,
            Measure measure,
            RetrieveProvider retrieveProvider,
            @Nullable MeasureEvalType measureEvalTypeOverride) {
        Interval measurementPeriod = measurementPeriod("2000-01-01", "2001-01-01");

        Library primaryLibrary = library(cql);
        measure.addLibrary(primaryLibrary.getId());

        LibraryManager ll = new LibraryManager(new ModelManager());
        ll.getLibrarySourceLoader().registerProvider(new StringLibrarySourceProvider(Collections.singletonList(cql)));

        var modelResolver = new CachingModelResolverDecorator(new R4FhirModelResolver());
        DataProvider dataProvider = new CompositeDataProvider(modelResolver, retrieveProvider);

        var dps = new HashMap<String, DataProvider>();
        dps.put(MeasureConstants.FHIR_MODEL_URI, dataProvider);

        // TODO: Set up engine environment
        var engine = new CqlEngine(new Environment(ll, dps, null));
        var id = new VersionedIdentifier().withId("Test");
        var lib = engine.getEnvironment().getLibraryManager().resolveLibrary(id);
        engine.getState().init(lib.getLibrary());

        var libraryEngine = new LibraryEngine(repository, evaluationOptions.getEvaluationSettings());

        R4MeasureEvaluation evaluation = new R4MeasureEvaluation(engine, measure, libraryEngine, id);
        MeasureReport report = evaluation.evaluate(
                getMeasureEvalType(subjectIds, measureEvalTypeOverride),
                subjectIds,
                measurementPeriod,
                libraryEngine,
                id);
        assertNotNull(report);

        // Simulate sending it across the wire
        IParser parser = FhirContext.forR4Cached().newJsonParser();
        report = (MeasureReport) parser.parseResource(parser.encodeResourceToString(report));

        return report;
    }

    @Nonnull
    private MeasureEvalType getMeasureEvalType(List<String> subjectIds, MeasureEvalType measureEvalTypeOverride) {
        return Optional.ofNullable(measureEvalTypeOverride)
                .orElse(subjectIds.size() == 1 ? MeasureEvalType.SUBJECT : MeasureEvalType.POPULATION);
    }

    private void checkEvidence(MeasureReport report) {
        Map<String, Resource> contained = report.getContained().stream()
                .collect(Collectors.toMap(r -> r.getClass().getSimpleName(), Function.identity()));

        assertEquals(1, contained.size());

        Observation obs = (Observation) contained.get("Observation");
        assertNotNull(obs);
        assertEquals(
                OMB_CATEGORY_RACE_BLACK,
                obs.getValueCodeableConcept().getCodingFirstRep().getCode());

        Optional<org.hl7.fhir.r4.model.Reference> optional = report.getEvaluatedResource().stream()
                .filter(x -> x.getReference().contains(obs.getId()))
                .findFirst();
        assertFalse(optional.isPresent());
    }

    private Measure cohort_measure() {

        Measure measure = measure("cohort");
        addPopulation(measure, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation");
        addSDEComponent(measure);

        return measure;
    }

    private Measure proportion_measure() {
        return proportion_measure(null, null);
    }

    private Measure proportion_measure(@Nullable CodeType populationBasisTypeForMeasure, @Nullable CodeType populationBasisTypeForGroup) {

        Measure measure = measure("proportion", populationBasisTypeForMeasure, populationBasisTypeForGroup);
        addPopulation(measure, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation");
        addPopulation(measure, MeasurePopulationType.DENOMINATOR, "Denominator");
        addPopulation(measure, MeasurePopulationType.NUMERATOR, "Numerator");
        addSDEComponent(measure);

        return measure;
    }

    private Measure continuous_variable_measure() {

        Measure measure = measure("continuous-variable");
        addPopulation(measure, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation");
        addPopulation(measure, MeasurePopulationType.MEASUREPOPULATION, "MeasurePopulation");
        addSDEComponent(measure);

        printResource(measure);

        return measure;
    }

    private Measure stratified_measure() {
        Measure measure = proportion_measure(null, null);
        addStratifier(measure, "patient-gender", "Gender");
        return measure;
    }

    private Measure stratified_measure(@Nullable CodeType populationBasisTypeForMeasure, @Nullable CodeType populationBasisTypeForGroup) {
        Measure measure = proportion_measure(populationBasisTypeForMeasure, populationBasisTypeForGroup);
        addStratifier(measure, "patient-gender", "Gender");
        return measure;
    }

    private void addStratifier(Measure measure, String stratifierId, String expression) {
        MeasureGroupStratifierComponent mgsc = measure.getGroupFirstRep().addStratifier();
        mgsc.getCriteria().setExpression(expression);
        mgsc.setId(stratifierId);
    }

    private void addPopulation(Measure measure, MeasurePopulationType measurePopulationType, String expression) {
        MeasureGroupPopulationComponent mgpc = measure.getGroupFirstRep().addPopulation();
        mgpc.setId(measurePopulationType.toCode());
        mgpc.getCode().getCodingFirstRep().setCode(measurePopulationType.toCode());
        mgpc.getCriteria().setExpression(expression);
    }

    private void addSDEComponent(Measure measure) {
        CodeableConcept cc = new CodeableConcept()
                .addCoding(new Coding(
                        "http://terminology.hl7.org/CodeSystem/measure-data-usage", "supplemental-data", null));
        MeasureSupplementalDataComponent sde = measure.getSupplementalDataFirstRep();
        sde.setId("sde-race");
        sde.getCode().setText("sde-race");
        sde.getUsage().add(cc);
        sde.getCriteria().setLanguage("text/cql").setExpression("SDE Race");
    }

    private Measure measure(String scoring) {
        return measure(scoring, null, null);
    }

    private Measure measure(String scoring, @Nullable CodeType populationBasisTypeForMeasure, @Nullable CodeType populationBasisTypeForGroup) {
        Measure measure = new Measure();
        measure.setId(scoring);
        measure.setName("Test");
        measure.setVersion("1.0.0");
        measure.setUrl("http://test.com/fhir/Measure/Test");
        measure.getScoring().getCodingFirstRep().setCode(scoring);
        Optional.ofNullable(populationBasisTypeForMeasure)
            .ifPresent(nonNullPopulationBasisType->
            measure.addExtension(new Extension()
                        .setUrl(MeasureConstants.POPULATION_BASIS_URL)
                        .setValue(populationBasisTypeForMeasure)));
        Optional.ofNullable(populationBasisTypeForGroup)
            .ifPresent(nonNullPopulationBasisType->
                measure.getGroupFirstRep()
                    .addExtension(new Extension()
                        .setUrl(MeasureConstants.POPULATION_BASIS_URL)
                        .setValue(populationBasisTypeForMeasure)));
        return measure;
    }

    private Library library(String cql) {
        Library library = new Library();
        library.setId("library-Test");
        library.setName("Test");
        library.setVersion("1.0.0");
        library.setUrl("http://test.com/fhir/Library/Test");
        library.getType().getCodingFirstRep().setCode("logic-library");
        library.getContentFirstRep().setContentType("text/cql").setData(cql.getBytes());
        return library;
    }

    private Patient john_doe() {
        Patient patient = new Patient();
        patient.setId(new IdType("Patient", "john-doe"));
        patient.setName(
                List.of(new HumanName().setFamily("Doe").setGiven(List.of(new StringType("John")))));
        patient.setBirthDate(new Date());
        patient.setGender(AdministrativeGender.MALE);

        /**
         * Retrieve the coding from an extension that that looks like the following...
         *
         * { "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race", "extension": [ {
         * "url": "ombCategory", "valueCoding": { "system": "urn:oid:2.16.840.1.113883.6.238", "code":
         * "2054-5", "display": "Black or African American" } } ] }
         */
        Extension usCoreRace = new Extension();
        usCoreRace
                .setUrl(EXT_URL_US_CORE_RACE)
                .addExtension()
                .setUrl(OMB_CATEGORY)
                .setValue(new Coding()
                        .setSystem(URL_SYSTEM_RACE)
                        .setCode(OMB_CATEGORY_RACE_BLACK)
                        .setDisplay(BLACK_OR_AFRICAN_AMERICAN));
        patient.getExtension().add(usCoreRace);

        return patient;
    }

    private Patient jane_doe() {
        Patient patient = new Patient();
        patient.setId(new IdType("Patient", "jane-doe"));
        patient.setName(
                List.of(new HumanName().setFamily("Doe").setGiven(List.of(new StringType("Jane")))));
        patient.setBirthDate(new Date());
        patient.setGender(AdministrativeGender.FEMALE);

        Extension usCoreRace = new Extension();
        usCoreRace
                .setUrl(EXT_URL_US_CORE_RACE)
                .addExtension()
                .setUrl(OMB_CATEGORY)
                .setValue(new Coding()
                        .setSystem(URL_SYSTEM_RACE)
                        .setCode(OMB_CATEGORY_RACE_BLACK)
                        .setDisplay(BLACK_OR_AFRICAN_AMERICAN));
        patient.getExtension().add(usCoreRace);
        return patient;
    }

    @Nonnull
    private RetrieveProvider setupMockRetrieverProvider() {
        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(
            isNull(),
            isNull(),
            isNull(),
            eq("Patient"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()))
            .thenReturn(Arrays.asList(jane_doe(), john_doe()));
        when(retrieveProvider.retrieve(
            eq("Patient"),
            eq("id"),
            eq("john-doe"),
            eq("Patient"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()))
            .thenReturn(List.of(john_doe()));
        when(retrieveProvider.retrieve(
            eq("Patient"),
            eq("id"),
            eq("jane-doe"),
            eq("Patient"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()))
            .thenReturn(List.of(jane_doe()));
        return retrieveProvider;
    }

    private void printResource(Resource resource) {
        final String json = FhirContext.forR4Cached()
            .newJsonParser()
            .setPrettyPrint(true)
            .encodeResourceToString(resource);

        System.out.println("json = \n" + json);
    }
}
