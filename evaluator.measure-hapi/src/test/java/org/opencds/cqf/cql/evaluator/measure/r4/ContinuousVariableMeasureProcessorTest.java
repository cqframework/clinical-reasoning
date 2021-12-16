package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;


@Test(singleThreaded = true)
public class ContinuousVariableMeasureProcessorTest extends BaseMeasureProcessorTest {

    public ContinuousVariableMeasureProcessorTest() {
        super("CMS111-bundle.json");
    }

    @Test
    public void cms111_singlePatient() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://ecqi.healthit.gov/ecqms/Measure/CMS111", "2019-01-01", "2020-01-01", "subject", "measure-strat1-EXM111", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "initial-population", 1);
    }
}
