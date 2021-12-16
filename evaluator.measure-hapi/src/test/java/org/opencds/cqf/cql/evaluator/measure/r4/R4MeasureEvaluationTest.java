package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.InMemoryLibraryLoader;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.measure.BaseMeasureEvaluationTest;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.testng.annotations.Test;

import ca.uhn.fhir.parser.IParser;


@Test(singleThreaded = true)
public class R4MeasureEvaluationTest extends BaseMeasureEvaluationTest {

    public String getFhirVersion() {
        return "4.0.1";
    }

    @Test
    public void testCohortMeasureEvaluation() throws Exception {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(eq("Patient"), anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(Arrays.asList(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n";

        Measure measure = cohort_measure();

        MeasureReport report = runTest(cql, patient, measure, retrieveProvider);
        checkEvidence(patient, report);
    }

    @Test
    public void testSDEInMeasureEvaluation() throws Exception {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(eq("Patient"), anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(Arrays.asList(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n";

        Measure measure = cohort_measure();

        MeasureReport report = runTest(cql, patient, measure, retrieveProvider);
        checkEvidence(patient, report);
    }

    @Test
    public void testProportionMeasureEvaluation() throws Exception {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(eq("Patient"), anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(Arrays.asList(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define Denominator: 'John' in Patient.name.given\n"
                + "define Numerator: Patient.birthDate > @1970-01-01\n";

        Measure measure = proportion_measure();

        MeasureReport report = runTest(cql, patient, measure, retrieveProvider);
        checkEvidence(patient, report);
    }

    @Test
    public void testProportionMeasureEvaluationWithDate() throws Exception {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(eq("Patient"), anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(Arrays.asList(patient));

        String cql = cql_with_date() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define Denominator: 'John' in Patient.name.given\n"
                + "define Numerator: AgeInYearsAt(start of \"Measurement Period\") > 18\n";

        Measure measure = proportion_measure();

        MeasureReport report = runTest(cql, patient, measure, retrieveProvider);
        checkEvidence(patient, report);
    }

    @Test
    public void testContinuousVariableMeasureEvaluation() throws Exception {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(eq("Patient"), anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(Arrays.asList(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define MeasurePopulation: Patient.birthDate > @1970-01-01\n";

        Measure measure = continuous_variable_measure();

        MeasureReport report = runTest(cql, patient, measure, retrieveProvider);
        checkEvidence(patient, report);
    }

    @Test
    public void testStratifiedMeasureEvaluation() throws Exception {
        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(isNull(), isNull(), isNull(), eq("Patient"), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(Arrays.asList(jane_doe(), john_doe()));
        when(retrieveProvider.retrieve(eq("Patient"), eq("id"), eq("john-doe"), eq("Patient"), any(), any(), any(),
                any(), any(), any(), any(), any())).thenReturn(Arrays.asList(john_doe()));
        when(retrieveProvider.retrieve(eq("Patient"), eq("id"), eq("jane-doe"), eq("Patient"), any(), any(), any(),
                any(), any(), any(), any(), any())).thenReturn(Arrays.asList(jane_doe()));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: 'Doe' in Patient.name.family\n"
                + "define Denominator: 'John' in Patient.name.given\n"
                + "define Numerator: Patient.birthDate > @1970-01-01\n" + "define Gender: Patient.gender\n";

        Measure measure = stratified_measure();

        MeasureReport report = runTest(cql, null, measure, retrieveProvider);
        checkStratification(report);

    }

    @Test
    public void testEvaluatePopulationCriteriaNullResult() throws Exception {
        Patient patient = john_doe();

        RetrieveProvider retrieveProvider = mock(RetrieveProvider.class);
        when(retrieveProvider.retrieve(eq("Patient"), anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(Arrays.asList(patient));

        String cql = cql_with_dateTime() + sde_race() + "define InitialPopulation: null\n"
                + "define Denominator: null\n"
                + "define Numerator: null\n";

        Measure measure = proportion_measure();

        MeasureReport report = runTest(cql, patient, measure, retrieveProvider);
        checkEvidence(patient, report);
    }

    private void checkStratification(MeasureReport report) {
        MeasureReportGroupStratifierComponent mrgsc = report.getGroupFirstRep().getStratifierFirstRep();
        assertEquals(mrgsc.getId(), "patient-gender");
        assertEquals(mrgsc.getStratum().size(), 2);

        StratifierGroupComponent sgc = mrgsc.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().getText().equals("male")).findFirst().get();
        StratifierGroupPopulationComponent sgpc = sgc.getPopulation().stream().filter(
                x -> x.getCode().getCodingFirstRep().getCode().equals(MeasurePopulationType.INITIALPOPULATION.toCode()))
                .findFirst().get();

        assertEquals(sgpc.getCount(), 1);

        sgc = mrgsc.getStratum().stream().filter(x -> x.hasValue() && x.getValue().getText().equals("female"))
                .findFirst().get();
        sgpc = sgc.getPopulation().stream().filter(
                x -> x.getCode().getCodingFirstRep().getCode().equals(MeasurePopulationType.INITIALPOPULATION.toCode()))
                .findFirst().get();

        assertEquals(sgpc.getCount(), 1);
    }

    private MeasureReport runTest(String cql, Patient patient, Measure measure, RetrieveProvider retrieveProvider)
            throws Exception {
        Interval measurementPeriod = measurementPeriod("2000-01-01", "2001-01-01");

        Library primaryLibrary = library(cql);
        measure.addLibrary(primaryLibrary.getId());

        List<org.cqframework.cql.elm.execution.Library> cqlLibraries = translate(cql);
        LibraryLoader ll = new InMemoryLibraryLoader(cqlLibraries);

        R4FhirModelResolver modelResolver = new R4FhirModelResolver();
        DataProvider dataProvider = new CompositeDataProvider(modelResolver, retrieveProvider);
        Context context = new Context(cqlLibraries.get(0));
        context.registerDataProvider(FHIR_NS_URI, dataProvider);
        context.registerLibraryLoader(ll);

        R4MeasureEvaluation evaluation = new R4MeasureEvaluation(context, measure);
        MeasureReport report = evaluation.evaluate(
                patient != null ? MeasureEvalType.SUBJECT : MeasureEvalType.POPULATION,
                patient != null ? patient.getId() : null, measurementPeriod);
        assertNotNull(report);

        // Simulate sending it across the wire
        IParser parser = modelResolver.getFhirContext().newJsonParser();
        report = (MeasureReport) parser.parseResource(parser.encodeResourceToString(report));

        return report;
    }

    private void checkEvidence(Patient patient, MeasureReport report) {
        Map<String, Resource> contained = report.getContained().stream()
                .collect(Collectors.toMap(r -> r.getClass().getSimpleName(), Function.identity()));

        assertEquals(contained.size(), 1);

        Observation obs = (Observation) contained.get("Observation");
        assertNotNull(obs);
        assertEquals(obs.getValueCodeableConcept().getCodingFirstRep().getCode(), OMB_CATEGORY_RACE_BLACK);

        Optional<org.hl7.fhir.r4.model.Reference> optional = report.getEvaluatedResource().stream().filter(x -> x.getReference().contains(obs.getId())).findFirst();
        assertTrue(optional.isPresent());
    }

    private Measure cohort_measure() {

        Measure measure = measure("cohort");
        addPopulation(measure, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation");
        addSDEComponent(measure);

        return measure;
    }

    private Measure proportion_measure() {

        Measure measure = measure("proportion");
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

        return measure;
    }

    private Measure stratified_measure() {
        Measure measure = proportion_measure();
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
        mgpc.getCode().getCodingFirstRep().setCode(measurePopulationType.toCode());
        mgpc.getCriteria().setExpression(expression);
    }

    private void addSDEComponent(Measure measure) {
        MeasureSupplementalDataComponent sde = measure.getSupplementalDataFirstRep();
        sde.getCode().setText("sde-race");
        sde.getCriteria().setLanguage("text/cql").setExpression("SDE Race");
    }

    private Measure measure(String scoring) {
        Measure measure = new Measure();
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
        patient.setId("john-doe");
        patient.setName(
                Arrays.asList(new HumanName().setFamily("Doe").setGiven(Arrays.asList(new StringType("John")))));
        patient.setBirthDate(new Date());
        patient.setGender(AdministrativeGender.MALE);

        /**
         * Retrieve the coding from an extension that that looks like the following...
         * 
         * { "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race",
         * "extension": [ { "url": "ombCategory", "valueCoding": { "system":
         * "urn:oid:2.16.840.1.113883.6.238", "code": "2054-5", "display": "Black or
         * African American" } } ] }
         */

        Extension usCoreRace = new Extension();
        usCoreRace.setUrl(EXT_URL_US_CORE_RACE).addExtension().setUrl(OMB_CATEGORY).setValue(new Coding()
                .setSystem(URL_SYSTEM_RACE).setCode(OMB_CATEGORY_RACE_BLACK).setDisplay(BLACK_OR_AFRICAN_AMERICAN));
        patient.getExtension().add(usCoreRace);

        return patient;
    }

    private Patient jane_doe() {
        Patient patient = new Patient();
        patient.setId("jane-doe");
        patient.setName(
                Arrays.asList(new HumanName().setFamily("Doe").setGiven(Arrays.asList(new StringType("Jane")))));
        patient.setBirthDate(new Date());
        patient.setGender(AdministrativeGender.FEMALE);

        Extension usCoreRace = new Extension();
        usCoreRace.setUrl(EXT_URL_US_CORE_RACE).addExtension().setUrl(OMB_CATEGORY).setValue(new Coding()
                .setSystem(URL_SYSTEM_RACE).setCode(OMB_CATEGORY_RACE_BLACK).setDisplay(BLACK_OR_AFRICAN_AMERICAN));
        patient.getExtension().add(usCoreRace);
        return patient;
    }
}
