package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class MeasureSupportingEvidenceTest {

    private static final Given given = Measure.given().repositoryFor("MeasureTest");

    @Test
    void ratioSupportingEvidence() {

        given.when()
                .measureId("RatioGroupBooleanAllPopulationsSuppEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .getExtDef("always true")
                .extensionDefHasResults()
                .up()
                .getExtDef("Denominator Resource")
                .extensionDefHasResults()
                .up()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore(1.0)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .getPopulationExtension("always true")
                .hasBooleanResult(true)
                .up()
                .getPopulationExtension("Denominator Resource")
                .hasResourceIdResult("Encounter/patient-9-encounter-2")
                .hasResourceIdResult("Encounter/patient-9-encounter-1")
                .up()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .report();
    }
}
