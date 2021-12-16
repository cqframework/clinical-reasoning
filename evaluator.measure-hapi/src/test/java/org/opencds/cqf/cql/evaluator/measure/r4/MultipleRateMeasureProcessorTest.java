package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;


@Test(singleThreaded = true)
public class MultipleRateMeasureProcessorTest extends BaseMeasureProcessorTest {
    public MultipleRateMeasureProcessorTest() {
        super("FHIR347-bundle.json");
    }

    @Test
    public void fhir347_singlePatient() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://ecqi.healthit.gov/ecqms/Measure/FHIR347", "2019-01-01", "2020-01-01", "subject", "numer1-EXM347", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "initial-population", 1);
    }
}
