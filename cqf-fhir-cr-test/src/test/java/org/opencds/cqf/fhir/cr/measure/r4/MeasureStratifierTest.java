package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.When;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * Measure Stratifier Testing to validate Measure defined Stratifier elements and the resulting MeasureReport Stratifier elements
 * Mainly using a Cohort Measure as the example scoringType for simplicity of parsing results.
 *
 */
@SuppressWarnings("squid:S2699")
class MeasureStratifierTest {
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(MeasureStratifierTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }
    /**
     * Boolean Basis Measure with Stratifier defined by component expression that results in CodeableConcept value of 'M' or 'F' for the Measure population. For 'Individual' reportType
     */
    @Test
    void cohortBooleanHasCodeStratIndividualResult() {
        var mCC = new CodeableConcept().setText("M");

        given.when()
                .measureId("CohortBooleanStratCode")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .stratumCount(1)
                .stratum(mCC)
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Stratifier defined by component expression that results in CodeableConcept value of 'M' or 'F' for the Measure population.
     */
    @Test
    void cohortBooleanHasCodeStrat() {
        var mCC = new CodeableConcept().setText("M");
        var fCC = new CodeableConcept().setText("F");

        given.when()
                .measureId("CohortBooleanStratCode")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .stratumCount(2)
                .stratum(mCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .stratum(fCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Stratifier defined by value expression that results in CodeableConcept value of 'true' or 'false' for the Measure population.
     */
    @Test
    void cohortBooleanValueStrat() {
        var isUnfinished = new CodeableConcept().setText("true");
        var notUnfinished = new CodeableConcept().setText("false");

        given.when()
                .measureId("CohortBooleanStratValue")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .stratumCount(2)
                .stratum(isUnfinished)
                .firstPopulation()
                .hasCount(9)
                .up()
                .up()
                .stratum(notUnfinished)
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Multiple Stratifiers defined by value expression & component expression.
     * Each Stratifier should have Stratum for varied results.
     * stratifier 1: 'true' or 'false' for the Measure population.
     * stratifier 2: 'M' or 'F' for the Measure population.
     */
    @Test
    void cohortBooleanMultiStrat() {
        var isUnfinished = new CodeableConcept().setText("true");
        var notUnfinished = new CodeableConcept().setText("false");
        var mCC = new CodeableConcept().setText("M");
        var fCC = new CodeableConcept().setText("F");

        given.when()
                .measureId("CohortBooleanStratMulti")
                .evaluate()
                .then()
                .firstGroup()
                .stratifierById("stratifier-1")
                .stratumCount(2)
                .stratum(isUnfinished)
                .firstPopulation()
                .hasCount(9)
                .up()
                .up()
                .stratum(notUnfinished)
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .stratifierById("stratifier-2")
                .stratumCount(2)
                .stratum(mCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .stratum(fCC)
                .firstPopulation()
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
    /**
     * Boolean Basis Measure with Stratifier defined without an 'id' populated. Result should throw an error.
     */
    @Test
    void cohortBooleanNoIdStrat() {
        final When evaluate = given.when().measureId("CohortBooleanStratNoId").evaluate();
        try {
            evaluate.then();
            fail("should throw a missing Id scenario");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("id is required on all Elements of type: Measure.group.stratifier"));
        }
    }

    // Previously, we didn't expect this to fail but with the new validation logic we decided that
    // it now ought to.
    @Test
    void cohortBooleanDifferentTypeStrat() {
        try {
            given.when().measureId("CohortBooleanStratDifferentType").evaluate().then();
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "stratifier expression criteria results for expression: [resource strat not finished] must fall within accepted types for boolean population basis: [boolean] for Measure: http://example.com/Measure/CohortBooleanStratDifferentType",
                    exception.getMessage());
        }
    }

    /**
     * Boolean Basis Measure with Stratifier defined as a component.
     * MultiComponent stratifiers blend results of multiple criteria (Gender of Patient and Payer, instead of just one or the other)
     * This is allowed within the specification, but is not currently implemented
     */
    @Test
    void cohortBooleanComponentStrat() {
        given.when()
                .measureId("CohortBooleanStratComponent")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("multi-component stratifiers are not yet supported")
                .report();
    }

    /**
     * Ratio Measure with Resource Basis where Stratifier defined by component expression that results in Encounter Resources for the Measure population.
     * Given that Population results are "Encounter" resources, intersection of results should be allowed
     */
    @Test
    void ratioResourceValueStrat() {

        given.when()
                .measureId("RatioResourceStratValue")
                .evaluate()
                .then()
                .firstGroup()
                .firstStratifier()
                .stratumCount(9) // one per Encounter resource
                .stratum("Encounter/patient-1-encounter-1")
                .hasScore("0.0") // make sure stratum score
                .firstPopulation()
                .hasCount(1)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Ratio Measure with Resource Basis where Stratifier defined by component expression that results in CodeableConcept value of 'M' or 'F' for the Measure population.
     * Given that Population results are "Encounter" resources, intersection of results with Patient.gender is not possible. All results would be empty
     * This should throw an error
     */
    @Test
    void ratioResourceDifferentTypeStrat() {
        given.when()
                .measureId("RatioResourceStratDifferentType")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg(
                        "stratifier expression criteria results for expression: [Gender Stratification] must fall within accepted types for population basis: [Encounter] for Measure: http://example.com/Measure/RatioResourceStratDifferentType")
                .report();
    }

    /**
     * Ratio Measure with Resource Basis where Stratifier defined by component expression that results in Encounter Resources for the Measure population.
     * Given that Population results are "Encounter" resources, intersection of results should be allowed
     */
    @Test
    void ratioBooleanValueStrat() {

        given.when()
                .measureId("RatioBooleanStratValue")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(10)
                .up()
                .firstStratifier()
                .stratumCount(2)
                .stratum("M")
                .hasScore("0.2") // make sure stratum are scored
                .population("initial-population")
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
}
