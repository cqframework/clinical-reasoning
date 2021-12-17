package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;


@Test(singleThreaded = true)
public class EncounterBasisMeasureProcessorTest extends BaseMeasureProcessorTest {
    public EncounterBasisMeasureProcessorTest() {
        super("DischargedonAntithromboticTherapyFHIR-bundle.json");
    }

    @Test
    public void exm104_singlePatient() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://ecqi.healthit.gov/ecqms/Measure/DischargedonAntithromboticTherapyFHIR", "2019-01-01", "2020-01-01", "subject", "numer-EXM104", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "initial-population", 1);
    }
}
