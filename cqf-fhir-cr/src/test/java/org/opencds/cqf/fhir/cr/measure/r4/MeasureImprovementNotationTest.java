package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings({"squid:S2699", "squid:S1135"})
class MeasureImprovementNotationTest {
    // undefined improvementNotation
    // measure defined
    // group defined
    // increase
    // decrease
    // invalid value
    // multi-group
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(MeasureImprovementNotationTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }

    @Test
    void proportionBooleanImprovementNotationNone() {
        // if ImprovementNotation is not defined then it defaults to 'increase'
        given.when()
                .measureId("ProportionBooleanImprovementNotationNone")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasNoReportLevelImprovementNotation() // when nothing defined
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanImprovementNotationNone")
                .firstGroup()
                .hasNoImprovementNotationExt()
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
                .hasCount(2) // because subject was also in Numerator
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
    void proportionBooleanImprovementNotationMeasureIncrease() {
        // if ImprovementNotation is on Measure level and code 'increase'
        // a numerator value being higher indicates improvement
        given.when()
                .measureId("ProportionBooleanImprovementNotationMeasureIncrease")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportLevelImprovementNotation() // validate has code
                .improvementNotationCode("increase") // validate correct code is present
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanImprovementNotationMeasureIncrease")
                .firstGroup()
                .hasNoImprovementNotationExt() // validate duplicate codes are not present
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
                .hasCount(2) // because subject was also in Numerator
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
    void proportionBooleanImprovementNotationMeasureDecrease() {
        // TODO: fix this
        // if ImprovementNotation is on Measure level and code 'increase'
        // a numerator value being lower indicates improvement
        given.when()
                .measureId("ProportionBooleanImprovementNotationMeasureDecrease")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasReportLevelImprovementNotation() // validate has code
                .improvementNotationCode("decrease") // validate correct code is present
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanImprovementNotationMeasureDecrease")
                .firstGroup()
                .hasNoImprovementNotationExt() // validate duplicate codes are not present
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
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.6666666666666667")
                .up()
                .report();
    }

    @Test
    void proportionBooleanImprovementNotationGroupIncrease() {

        // if ImprovementNotation is on Measure level and code 'increase'
        // a numerator value being lower indicates improvement
        given.when()
                .measureId("ProportionBooleanImprovementNotationGroupIncrease")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasNoReportLevelImprovementNotation()
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanImprovementNotationGroupIncrease")
                .firstGroup()
                .hasImprovementNotationExt("increase")
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
                .hasCount(2) // because subject was also in Numerator
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
    void proportionBooleanImprovementNotationGroupDecrease() {

        // if ImprovementNotation is on Measure level and code 'decrease'
        // a numerator value being lower indicates improvement
        given.when()
                .measureId("ProportionBooleanImprovementNotationGroupDecrease")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasNoReportLevelImprovementNotation()
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanImprovementNotationGroupDecrease")
                .firstGroup()
                .hasImprovementNotationExt("decrease")
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
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.6666666666666667") // inverse score of increase
                .up()
                .report();
    }

    @Test
    void proportionBooleanImprovementNotationMeasureAndGroup() {

        // if ImprovementNotation is on Measure level and code 'increase'
        // and Group contains 'decrease', Logic should prioritize Group Definition
        // result should be 'decrease'

        given.when()
                .measureId("ProportionBooleanImprovementNotationMeasureAndGroup")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasNoReportLevelImprovementNotation()
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanImprovementNotationMeasureAndGroup")
                .firstGroup()
                .hasImprovementNotationExt("decrease")
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
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.6666666666666667") // inverse score of increase
                .up()
                .report();
    }

    @Test
    void proportionBooleanImprovementNotationMeasureInvalid() {

        // if ImprovementNotation is on Measure level and code 'increase'
        // and Group contains 'decrease', Logic should prioritize Group Definition
        // result should be 'decrease'
        try {
            given.when()
                    .measureId("ProportionBooleanImprovementNotationMeasureInvalid")
                    .reportType("population")
                    .evaluate()
                    .then()
                    .hasEmptySubject() // only for All subjects
                    .improvementNotationCode("neutral")
                    .hasReportType("Summary")
                    .hasMeasureReportDate()
                    .hasMeasureReportPeriod()
                    .hasStatus(MeasureReportStatus.COMPLETE)
                    .hasMeasureUrl("http://example.com/Measure/ProportionBooleanImprovementNotationMeasureInvalid")
                    .firstGroup()
                    .hasNoImprovementNotationExt()
                    .population("initial-population")
                    .hasCount(10)
                    .up()
                    .population("denominator")
                    .hasCount(6)
                    .up()
                    .population("denominator-exclusion")
                    .hasCount(2)
                    .up()
                    .population("denominator-exception")
                    .hasCount(2) // because subject was also in Numerator
                    .up()
                    .population("numerator-exclusion")
                    .hasCount(2)
                    .up()
                    .population("numerator")
                    .hasCount(2)
                    .up()
                    .hasScore("0.6666666666666667") // inverse score of increase
                    .up()
                    .report();
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "ImprovementNotation Coding has invalid System: http://terminology.hl7.org/CodeSystem/measure-improvement-notation, code: neutral, combination for Measure: http://example.com/Measure/ProportionBooleanImprovementNotationMeasureInvalid"));
        }
    }
    // ProportionBooleanMultiGroupImprovementNotation
    @Test
    void proportionBooleanMultiGroupImprovementNotation() {

        // if ImprovementNotation is on Measure level and code 'increase'
        // and Group contains 'decrease', Logic should prioritize Group Definition
        // result should be 'decrease'

        given.when()
                .measureId("ProportionBooleanMultiGroupImprovementNotation")
                .reportType("population")
                .evaluate()
                .then()
                .hasEmptySubject() // only for All subjects
                .hasNoReportLevelImprovementNotation()
                .hasReportType("Summary")
                .hasMeasureReportDate()
                .hasMeasureReportPeriod()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasMeasureUrl("http://example.com/Measure/ProportionBooleanMultiGroupImprovementNotation")
                .group("increase")
                .hasImprovementNotationExt("increase")
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
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.3333333333333333") // inverse score of increase
                .up()
                .group("decrease")
                .hasImprovementNotationExt("decrease")
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
                .hasCount(2) // because subject was also in Numerator
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .hasScore("0.6666666666666667") // inverse score of increase
                .up()
                .report();
    }
}
