package org.opencds.cqf.cql.evaluator.measure.dstu3;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.hl7.fhir.dstu3.model.Bundle;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class Dstu3MeasureAdditionalDataTest extends BaseMeasureProcessorTest {
    public Dstu3MeasureAdditionalDataTest() {
        super("EXM105FHIR3MeasurePartBundle.json");
    }

    @Test
    public void testMeasureAdditionalData() {
        var subject = "Patient/denom-EXM105-FHIR3";
        var parser = this.fhirContext.newJsonParser();
        var additionalData = (Bundle) parser
                .parseResource(Dstu3MeasureAdditionalDataTest.class
                        .getResourceAsStream("EXM105FHIR3MeasureAdditionalBundle.json"));
        var report = this.measureProcessor.evaluateMeasure(
                "http://hl7.org/fhir/us/cqfmeasures/Measure/EXM105-FHIR3-8.0.000",
                "2019-01-01", "2020-01-01", "subject", subject, null, null, endpoint, endpoint,
                null,
                additionalData);

        assertNotNull(report);
        assertEquals(subject, report.getPatient().getReference());
    }
}
