package org.opencds.cqf.fhir.cr.measure.r4;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.cr.measure.common.HashSetForFhirResourcesAndCqlTypes;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * This test is intended to test the behaviour of {@link IgRepository} as though it's doing
 * database queries, and returning new instances of resources each time, rather than the same
 * instance in memory. * It effectively copies tests that were failing when swapping the behaviour,
 * and running them under these conditions.  In other words, these tests prove the changes to use
 * {@link HashSetForFhirResourcesAndCqlTypes} actually work.
 */
@SuppressWarnings({"java:S2699"})
class MeasureEvaluationApplyScoringTests {

    // These are "IG"'s used in other tests:
    private static final String IG_NAME_MEASURE_TEST = "MeasureTest";
    private static final String IG_NAME_MINIMAL_MEASURE_EVALUATION = "MinimalMeasureEvaluation";

    private static final Measure.Given GIVEN_SINGLE = getGivenWithMockedRepositorySingleMeasure();

    private static final MultiMeasure.Given GIVEN_MULTI = getGivenWithMockedRepositoryMinimalMeasure();

    @Test
    void proportionResourceWithReportTypeParameterPatientGroup() {
        // Patients in Group
        GIVEN_SINGLE
                .when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .subject("Group/group-patients-1")
                .evaluate()
                .then()
                .hasSubjectReference("Group/group-patients-1")
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithReportTypeParameterPractitionerGroup() {
        // Patients with generalPractitioner.reference matching member of group
        GIVEN_SINGLE
                .when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .subject("Group/group-practitioners-1")
                .evaluate()
                .then()
                .hasSubjectReference("Group/group-practitioners-1")
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithReportTypeParameterPractitioner() {
        // Patients with generalPractitioner.reference matching member of group
        GIVEN_SINGLE
                .when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .subject("Practitioner/practitioner-1")
                .evaluate()
                .then()
                .hasSubjectReference("Practitioner/practitioner-1")
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithNoReportType() {
        // this should default to 'Summary' for empty subject
        GIVEN_SINGLE
                .when()
                .measureId("ProportionResourceAllPopulations")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void proportionResourceWithReportTypeParameterEmptySubject() {
        // All subjects
        GIVEN_SINGLE
                .when()
                .measureId("ProportionResourceAllPopulations")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionResourceAllPopulations")
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(3) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333")
                .up()
                .report();
    }

    @Test
    void MultiMeasure_EightMeasures_AllSubjects_MeasureUrl() {
        var when = GIVEN_MULTI
                .when()
                .measureUrl("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .measureUrl("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                .hasMeasureReportCount(7)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(7)
                .up()
                .hasScore("0.5")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(6)
                .up()
                .hasScore("0.375")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .hasMeasureReportStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Patient/female-1988-2")
                .hasContainedOperationOutcomeMsg("Invalid Interval")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(2)
                .up()
                .population("measure-observation")
                .hasCount(10);
    }

    // This test is for a Measure that references CQL with an invalid "MeasureObservation" function that returns an
    // Encounter instead of String, Integer or Double
    @Test
    void ContinuousVariableResourceMeasureObservationFunctionReturnsEncounterINVALID() {
        GIVEN_MULTI
                .when()
                .measureId("MinimalContinuousVariableResourceBasisSingleGroupINVALID")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate()
                .then()
                .hasMeasureReportCount(1)
                .getFirstMeasureReport()
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "continuous variable observation CQL \"MeasureObservation\" function result must be of type String, Integer or Double but was: Encounter");
    }

    @Test
    void MultiMeasure_EightMeasures_AllSubjects_MeasureId() {
        var when = GIVEN_MULTI
                .when()
                .measureId("MinimalProportionNoBasisSingleGroup")
                .measureId("MinimalProportionBooleanBasisSingleGroup")
                .measureId("MinimalRatioBooleanBasisSingleGroup")
                .measureId("MinimalRatioResourceBasisSingleGroup")
                .measureId("MinimalCohortResourceBasisSingleGroup")
                .measureId("MinimalCohortBooleanBasisSingleGroup")
                .measureId("MinimalContinuousVariableResourceBasisSingleGroup")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                .hasMeasureReportCount(7)
                .measureReport("http://example.com/Measure/MinimalProportionNoBasisSingleGroup")
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalProportionBooleanBasisSingleGroup")
                .hasReportType("Summary")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("denominator-exception")
                .hasCount(1)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(7)
                .up()
                .hasScore("0.5714285714285714")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioBooleanBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(7)
                .up()
                .hasScore("0.5")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalRatioResourceBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("denominator")
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(3)
                .up()
                .population("numerator")
                .hasCount(6)
                .up()
                .hasScore("0.375")
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortResourceBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/MinimalContinuousVariableResourceBasisSingleGroup")
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("measure-population")
                .hasCount(10)
                .up()
                .population("measure-population-exclusion")
                .hasCount(2)
                .up()
                .population("measure-observation")
                .hasCount(10)
                .up()
                .up()
                .hasMeasureReportStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Patient/female-1988-2")
                .hasContainedOperationOutcomeMsg("Invalid Interval");
    }

    // Use this if you want database-like behaviour of retrieving different objects per query
    private static Measure.Given getGivenWithMockedRepositorySingleMeasure() {
        var origGiven = Measure.given().repositoryFor(IG_NAME_MEASURE_TEST);

        var spiedRepository = spy(origGiven.getRepository());

        setupMocks(spiedRepository);

        return Measure.given().repository(spiedRepository);
    }

    // Use this if you want database-like behaviour of retrieving different objects per query
    private static MultiMeasure.Given getGivenWithMockedRepositoryMinimalMeasure() {
        var origGiven = MultiMeasure.given().repositoryFor(IG_NAME_MINIMAL_MEASURE_EVALUATION);

        var spiedRepository = spy(origGiven.getRepository());

        setupMocks(spiedRepository);

        return MultiMeasure.given().repository(spiedRepository);
    }

    private static void setupMocks(IRepository spiedRepository) {
        final Answer<Object> readAnswer = invocation -> {
            var realResult = invocation.callRealMethod();

            // If they're not Measures, we don't care for now
            if (realResult instanceof org.hl7.fhir.r4.model.Measure measure) {
                return cloneMeasure(measure);
            }

            if (realResult instanceof Encounter encounter) {
                return cloneEncounter(encounter);
            }

            return realResult;
        };

        final Answer<Object> searchAnswer = invocation -> {
            var realResult = invocation.callRealMethod();

            // If they're not Measures, we don't care for now
            if (!(realResult instanceof Bundle bundle)) {
                return realResult;
            }

            var bundleEntries = bundle.getEntry();

            final Predicate<IBaseResource> measurePredicate = org.hl7.fhir.r4.model.Measure.class::isInstance;
            final Predicate<IBaseResource> encounterPredicate = Encounter.class::isInstance;

            var hasAnyMeasures = bundleEntries.stream()
                    .map(BundleEntryComponent::getResource)
                    .anyMatch(measurePredicate.or(encounterPredicate));

            if (!hasAnyMeasures) {
                return realResult;
            }

            var newBundle = new Bundle();

            for (var entry : bundleEntries) {
                var resource = entry.getResource();
                if (resource instanceof org.hl7.fhir.r4.model.Measure measure) {
                    newBundle.addEntry(new BundleEntryComponent().setResource(cloneMeasure(measure)));
                } else if (resource instanceof Encounter encounter) {
                    newBundle.addEntry(new BundleEntryComponent().setResource(cloneEncounter(encounter)));
                } else {
                    newBundle.addEntry(entry);
                }
            }
            return newBundle;
        };

        doAnswer(readAnswer)
                .when(spiedRepository)
                .read(eq(org.hl7.fhir.r4.model.Measure.class), any(IdType.class), ArgumentMatchers.any());

        doAnswer(searchAnswer)
                .when(spiedRepository)
                .search(
                        eq(Bundle.class),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.<Multimap<String, List<IQueryParameterType>>>any(),
                        ArgumentMatchers.any());
    }

    // Lack of import aliases in Java is irritating
    private static org.hl7.fhir.r4.model.Encounter cloneEncounter(org.hl7.fhir.r4.model.Encounter encounter) {
        // A limited shallow clone is fine for our purposes
        var clonedEncounter = (org.hl7.fhir.r4.model.Encounter) new org.hl7.fhir.r4.model.Encounter()
                .setIdentifier(encounter.getIdentifier())
                .setPartOf(encounter.getPartOf())
                .setPeriod(encounter.getPeriod())
                .setStatus(encounter.getStatus())
                .setSubject(encounter.getSubject())
                .setType(encounter.getType())
                .setId(encounter.getId());

        clonedEncounter.setExtension(encounter.getExtension());

        return clonedEncounter;
    }

    // Lack of import aliases in Java is irritating
    private static org.hl7.fhir.r4.model.Measure cloneMeasure(org.hl7.fhir.r4.model.Measure measure) {
        // A limited shallow clone is fine for our purposes
        var clonedMeasure = (org.hl7.fhir.r4.model.Measure) new org.hl7.fhir.r4.model.Measure()
                .setDate(measure.getDate())
                .setGroup(measure.getGroup())
                .setIdentifier(measure.getIdentifier())
                .setLibrary(measure.getLibrary())
                .setName(measure.getName())
                .setScoring(measure.getScoring())
                .setStatus(measure.getStatus())
                .setSubject(measure.getSubject())
                .setType(measure.getType())
                .setVersion(measure.getVersion())
                .setUrl(measure.getUrl())
                .setId(measure.getId());

        clonedMeasure.setExtension(measure.getExtension());

        return clonedMeasure;
    }
}
