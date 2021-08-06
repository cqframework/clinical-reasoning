package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

public class StratifierMeasureProcessorTest extends BaseMeasureProcessorTest {
    public StratifierMeasureProcessorTest() {
        super("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR-bundle.json");
    }

    @Test
    public void stratifiers_exm74() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://ecqi.healthit.gov/ecqms/Measure/PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR", "2019-01-01", "2020-01-01", "subject", "denom-EXM74-strat1-case1", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "initial-population", 1);
    }
}
