package org.opencds.cqf.cql.evaluator.measure.dstu3;

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class Dstu3MeasureProcessorTest extends BaseMeasureProcessorTest {
    public Dstu3MeasureProcessorTest() {
        super("EXM105FHIR3Measure.json");
    }

    @Test
    public void testMeasureEvaluate() {
        var report = this.measureProcessor.evaluateMeasure(
                "http://hl7.org/fhir/us/cqfmeasures/Measure/EXM105-FHIR3-8.0.000", "2019-01-01",
                "2020-01-01", "subject", "Patient/denom-EXM105-FHIR3", null, null, endpoint,
                endpoint, endpoint, null);

        assertNotNull(report);
    }
}
