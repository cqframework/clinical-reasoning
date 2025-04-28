package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Environment;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.BaseMeasureEvaluationTest;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class Dstu3MeasureEvaluationTest extends BaseMeasureEvaluationTest {

    public String getFhirVersion() {
        return "3.0.0";
    }

    private IRepository repository = new IgRepository(
            FhirContext.forDstu3Cached(),
            Paths.get(getResourcePath(this.getClass()) + "/org/opencds/cqf/fhir/cr/measure/dstu3/EXM105FHIR3Measure/"));
    private MeasureEvaluationOptions evaluationOptions = MeasureEvaluationOptions.defaultOptions();

    @Test
    void cohortMeasureEvaluation() throws Exception {
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

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family.value";

        Measure measure = cohort_measure();

        MeasureReport report = runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider);

        checkEvidence(report);
    }

    @Test
    void proportionMeasureEvaluation() throws Exception {
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

        String cql = cql_with_dateTime() + sde_race()
                + "define InitialPopulation: 'Doe' in Patient.name.family.value\n"
                + "define Denominator: 'John' in Patient.name.given.value\n"
                + "define Numerator: Patient.birthDate > @1970-01-01\n";

        Measure measure = proportion_measure();

        MeasureReport report = runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider);
        checkEvidence(report);
    }

    @Test
    void continuosVariableMeasureEvaluation() throws Exception {
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

        String cql = cql_with_dateTime() + sde_race()
                + "define InitialPopulation: 'Doe' in Patient.name.family.value\n"
                + "define MeasurePopulation: Patient.birthDate > @1970-01-01\n";

        Measure measure = continuous_variable_measure();

        MeasureReport report = runTest(cql, Collections.singletonList(patient.getId()), measure, retrieveProvider);
        checkEvidence(report);
    }

    @Test
    void stratifiedMeasureEvaluation() throws Exception {
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
                .thenReturn(Arrays.asList(john_doe()));
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
                .thenReturn(Arrays.asList(jane_doe()));

        String cql = cql_with_dateTime() + sde_race()
                + "define InitialPopulation: 'Doe' in Patient.name.family.value\n"
                + "define Denominator: 'John' in Patient.name.given.value\n"
                + "define Numerator: Patient.birthDate > @1970-01-01\n" + "define Gender: Patient.gender\n";

        Measure measure = stratified_measure();

        MeasureReport report =
                runTest(cql, Arrays.asList(jane_doe().getId(), john_doe().getId()), measure, retrieveProvider);
        checkStratification(report);
    }

    private MeasureReport runTest(
            String cql, List<String> subjectIds, Measure measure, RetrieveProvider retrieveProvider) throws Exception {
        Interval measurementPeriod = measurementPeriod("2000-01-01", "2001-01-01");

        Library primaryLibrary = library(cql);
        measure.addLibrary(new Reference(primaryLibrary));

        LibraryManager ll = new LibraryManager(new ModelManager());
        ll.getLibrarySourceLoader().registerProvider(new StringLibrarySourceProvider(Collections.singletonList(cql)));

        var modelResolver = new CachingModelResolverDecorator(new Dstu3FhirModelResolver());
        DataProvider dataProvider = new CompositeDataProvider(modelResolver, retrieveProvider);

        var dps = new HashMap<String, DataProvider>();
        dps.put(MeasureConstants.FHIR_MODEL_URI, dataProvider);

        var engine = new CqlEngine(new Environment(ll, dps, null));
        var id = new VersionedIdentifier().withId("Test");
        var lib = engine.getEnvironment().getLibraryManager().resolveLibrary(id);
        engine.getState().init(lib.getLibrary());
        var libraryEngine = new LibraryEngine(repository, this.evaluationOptions.getEvaluationSettings());
        Dstu3MeasureEvaluation evaluation = new Dstu3MeasureEvaluation(engine, measure, libraryEngine, id);
        MeasureReport report = evaluation.evaluate(
                subjectIds.size() == 1 ? MeasureEvalType.PATIENT : MeasureEvalType.POPULATION,
                subjectIds,
                measurementPeriod,
                libraryEngine,
                id);
        assertNotNull(report);

        // Simulate sending it across the wire
        IParser parser = FhirContext.forDstu3Cached().newJsonParser();
        report = (MeasureReport) parser.parseResource(parser.encodeResourceToString(report));
        return report;
    }

    private void checkEvidence(MeasureReport report) {
        assertNotNull(report.getEvaluatedResources());
        assertNotNull(report.getEvaluatedResources().getReference());
        String listRef = report.getEvaluatedResources().getReference();

        // The Observation for the SDE and the list of references
        assertEquals(2, report.getContained().size());
        Map<String, Resource> contained = report.getContained().stream()
                .collect(Collectors.toMap(r -> r.getClass().getSimpleName(), Function.identity()));

        ListResource list = (ListResource) contained.get("ListResource");
        assertEquals(list.getIdElement().getIdPart(), listRef);

        Observation obs = (Observation) contained.get("Observation");
        assertEquals(
                OMB_CATEGORY_RACE_BLACK,
                obs.getValueCodeableConcept().getCodingFirstRep().getCode());
    }

    private void checkStratification(MeasureReport report) {
        MeasureReportGroupStratifierComponent mrgsc = report.getGroupFirstRep().getStratifierFirstRep();
        assertEquals("patient-gender", mrgsc.getId());
        assertEquals(2, mrgsc.getStratum().size());

        StratifierGroupComponent sgc = mrgsc.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().equals("male"))
                .findFirst()
                .get();
        StratifierGroupPopulationComponent sgpc = sgc.getPopulation().stream()
                .filter(x -> x.getCode()
                        .getCodingFirstRep()
                        .getCode()
                        .equals(MeasurePopulationType.INITIALPOPULATION.toCode()))
                .findFirst()
                .get();

        assertEquals(1, sgpc.getCount());

        sgc = mrgsc.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().equals("female"))
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

    private Measure cohort_measure() {

        Measure measure = measure("cohort");
        addGroup(measure, "group-1");
        addPopulation(measure, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation");
        addSDE(measure, "sde-race", "SDE Race");

        return measure;
    }

    private Measure proportion_measure() {

        Measure measure = measure("proportion");
        addGroup(measure, "group-1");
        addPopulation(measure, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation");
        addPopulation(measure, MeasurePopulationType.DENOMINATOR, "Denominator");
        addPopulation(measure, MeasurePopulationType.NUMERATOR, "Numerator");
        addSDE(measure, "sde-race", "SDE Race");

        return measure;
    }

    private Measure continuous_variable_measure() {

        Measure measure = measure("continuous-variable");
        addGroup(measure, "group-1");
        addPopulation(measure, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation");
        addPopulation(measure, MeasurePopulationType.MEASUREPOPULATION, "MeasurePopulation");
        addSDE(measure, "sde-race", "SDE Race");

        return measure;
    }

    private Measure stratified_measure() {
        Measure measure = proportion_measure();
        addStratifier(measure, "patient-gender", "Gender");
        return measure;
    }

    private void addGroup(Measure measure, String groupId) {
        measure.addGroup().setId(groupId);
    }

    private void addStratifier(Measure measure, String stratifierId, String expression) {
        MeasureGroupStratifierComponent mgsc = measure.getGroupFirstRep().addStratifier();
        mgsc.setCriteria(expression);
        mgsc.setId(stratifierId);
    }

    private void addSDE(Measure measure, String sdeId, String expression) {
        MeasureSupplementalDataComponent sde = measure.addSupplementalData();
        sde.setId(sdeId);
        sde.setCriteria(expression);
    }

    private void addPopulation(Measure measure, MeasurePopulationType populationType, String expression) {
        MeasureGroupPopulationComponent mgpc = measure.getGroupFirstRep().addPopulation();
        mgpc.getCode().getCodingFirstRep().setCode(populationType.toCode());
        mgpc.setCriteria(expression);
        mgpc.setId(populationType.toCode());
    }

    private Measure measure(String scoring) {
        Measure measure = new Measure();
        measure.setId(scoring);
        measure.setName("Test");
        measure.setVersion("1.0.0");
        measure.setUrl("http://test.com/fhir/Measure/Test");
        measure.getScoring().getCodingFirstRep().setCode(scoring);
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
                Arrays.asList(new HumanName().setFamily("Doe").setGiven(Arrays.asList(new StringType("John")))));
        patient.setBirthDate(new Date());
        patient.setGender(AdministrativeGender.MALE);

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
                Arrays.asList(new HumanName().setFamily("Doe").setGiven(Arrays.asList(new StringType("Jane")))));
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
}
