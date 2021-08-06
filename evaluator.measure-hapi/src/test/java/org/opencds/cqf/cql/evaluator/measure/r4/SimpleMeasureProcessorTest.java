package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

public class SimpleMeasureProcessorTest extends BaseMeasureProcessorTest {
    
    public SimpleMeasureProcessorTest() {
        super("EXM108-8.3.000-bundle.json");
    }

    @Test
    public void partialSubjectId_exm108() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "subject", "numer-EXM108", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);

        report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "subject", "denom-EXM108", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 0);
        validateGroup(report.getGroup().get(0), "denominator", 1);
    }

    @Test
    public void fullSubjectId_exm108() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "subject", "Patient/numer-EXM108", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);

    }
}
