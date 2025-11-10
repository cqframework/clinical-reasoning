package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * the purpose of this test is to validate the output and required fields for evaluating a Measure with population basis that is neither resourceType or boolean
 */
@SuppressWarnings("squid:S2699")
class ApplyScoringSetMembershipTest {
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
            Path.of(getResourcePath(ApplyScoringSetMembershipTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given(false).repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }
    /**
     * scoring set membership set to false,
     * ("numerator")/("denominator" - "denominator-exclusion") => 1/(1-0) => NA
     * Result should omit score because it can't be calculated
     * This also validates that exclusion criteria is not removed from denominator or numerator
     */
    @Test
    void datePopulation() {

        given.when()
                .measureId("ProportionDatePopulationBasis")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                // Same interval for all 10 generated patients, should be a count of 10
                .hasCount(10)
                .up()
                .population("denominator")
                // Same interval for all 10 generated patients, should be a count of 10
                .hasCount(10)
                .up()
                .population("denominator-exclusion")
                // Same interval for all 10 generated patients, should be a count of 10
                .hasCount(10)
                .up()
                .population("numerator")
                // Same interval for all 10 generated patients, should be a count of 10
                .hasCount(10)
                .up()
                .hasMeasureScore(false)
                .up()
                .report();
    }

    /**
     * scoring set membership set to false,
     * ("numerator")/("denominator") => 1/(1) => 1.0
     * Result should have score because it can be calculated
     * This also validates that exclusion criteria is not removed from denominator or numerator
     */
    @Test
    void datePopulationHasScore() {

        given.when()
                .measureId("ProportionDatePopulationBasisNoExcl")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                // Same interval for all 10 generated patients, should be a count of 10
                .hasCount(10)
                .up()
                .population("denominator")
                // Same interval for all 10 generated patients, should be a count of 10
                .hasCount(10)
                .up()
                .population("numerator")
                // Same interval for all 10 generated patients, should be a count of 10
                .hasCount(10)
                .up()
                .hasMeasureScore(true)
                .hasScore("1.0")
                .up()
                .report();
    }
}
