package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import java.nio.file.Path;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

/**
 * the purpose of this test is to validate the output and required fields for evaluating MeasureScoring type Ratio
 */
@SuppressWarnings("squid:S2699")
class MeasureScoringTypeRatioContVariableTest {
    // req'd populations
    // exception works
    // exclusion works
    // has score
    // resource based
    // boolean based
    // group scoring def
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final IRepository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Path.of(getResourcePath(MeasureScoringTypeRatioContVariableTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
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
    void ratioContinuousVariableResourceBasis() {

        given.when()
                .measureId("RatioContVarResourceSum")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(11)
                .up()
                .population("denominator")
                .hasCount(11)//final Denominator = 9 (11-2)
                .up()
                .population("denominator-exclusion")
                .hasCount(2)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(2)// final Numerator = 2
                .up()
                .populationId("observation-den")
                .hasCount(9)// we remove exclusions in these counts so users can see final Observation count used
                .up()
                .populationId("observation-num")
                .hasCount(2)// we remove exclusions in these counts so users can see final Observation count used
                .up()
                .hasScore("0.2222222222222222")
                .up()
                .report();
    }


}
