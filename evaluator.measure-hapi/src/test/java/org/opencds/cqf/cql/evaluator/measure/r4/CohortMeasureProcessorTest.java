package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;


@Test(singleThreaded = true)
public class CohortMeasureProcessorTest extends BaseMeasureProcessorTest {
    public CohortMeasureProcessorTest() {
        super("SeenPatients-bundle.json");
    }

    @Test
    public void seenPatients_singlePatient() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/chronic-ds/Measure/SeenPatients", "2019-01-01", "2019-12-31", "subject", "ip-SeenPatients", null, null, endpoint, endpoint, endpoint, null);
        
        // Cohort Measures only have one population
        assertEquals(report.getGroup().size(), 1);
        validateGroup(report.getGroup().get(0), "initial-population", 1);
        assertEquals(report.getGroup().get(0).getPopulation().size(), 1);

        // TODO: Measure observations
    }
}
