package org.opencds.cqf.cql.evaluator.measure.r4;

import java.math.BigDecimal;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;


@Test(singleThreaded = true)
public class DaVinciExtensionMeasureProcessorTest extends BaseMeasureProcessorTest {
    
    public DaVinciExtensionMeasureProcessorTest() {
        super("BreastCancerScreeningFHIR-bundle.json");
    }

    @Test
    public void exm125_numerator() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR", "2019-01-01", "2019-12-31", "subject", "numer-EXM125", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroupFirstRep(), "numerator", 1);
        validateGroup(report.getGroupFirstRep(), "denominator", 1);

        validateGroupScore(report.getGroupFirstRep(), new BigDecimal("1.0"));

        validateEvaluatedResourceExtension(report.getEvaluatedResource(), "Patient/numer-EXM125", "initial-population", "denominator-exclusion");
        validateEvaluatedResourceExtension(report.getEvaluatedResource(), "DiagnosticReport/numer-EXM125-3", "numerator");
    }

}
