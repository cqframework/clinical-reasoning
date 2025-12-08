package org.opencds.cqf.fhir.cr.measure.fhir2deftest.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.Fhir2DefUnifiedMeasureTestHandler;

/**
 * DSTU3-specific Fhir2Def integration tests inspired by Dstu3MeasureAdditionalDataTest.
 * <p>
 * Tests the unified, version-agnostic test DSL handler for DSTU3 measure evaluation
 * with additional data, focusing on Def object capture and assertion capabilities.
 * </p>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class Dstu3MeasureAdditionalDataFhir2DefTest {

    /**
     * Test DSTU3 measure evaluation with additional data and Def capture.
     * <p>
     * This test verifies:
     * <ul>
     *   <li>Def capture works correctly for DSTU3</li>
     *   <li>The structure exists (4 populations)</li>
     *   <li>PopulationDef.getCount() works with streamlined logic (no GroupDef parameter needed)</li>
     *   <li>Population counts match expected values (numerator=0, denominator=1)</li>
     * </ul>
     * </p>
     * <p>
     * Based on: Dstu3MeasureAdditionalDataTest#measureAdditionalData
     * </p>
     */
    @Test
    void measureAdditionalData() {
        var parser = FhirContext.forDstu3Cached().newJsonParser();
        var additionalData = (Bundle)
                parser.parseResource(
                        Dstu3MeasureAdditionalDataFhir2DefTest.class.getResourceAsStream(
                                "/org/opencds/cqf/fhir/cr/measure/dstu3/EXM105FHIR3MeasurePartBundle/EXM105FHIR3MeasureAdditionalBundle.json"));

        Fhir2DefUnifiedMeasureTestHandler.given()
                .repositoryFor("dstu3/EXM105FHIR3MeasurePartBundle", FhirVersionEnum.DSTU3)
                .when()
                .measureId("measure-EXM105-FHIR3-8.0.000")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/denom-EXM105-FHIR3")
                .reportType("subject")
                .additionalData(additionalData)
                .captureDef()
                .evaluate()
                .then()
                .def()
                .hasMeasureId("measure-EXM105-FHIR3-8.0.000")
                .firstGroup()
                .hasPopulationCount(4)
                .population("numerator")
                .hasCount(0)
                .up()
                .population("denominator")
                .hasCount(1);
    }
}
