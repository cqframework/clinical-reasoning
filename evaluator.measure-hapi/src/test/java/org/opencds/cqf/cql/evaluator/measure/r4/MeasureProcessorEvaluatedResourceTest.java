package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.StringType;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;
import static org.testng.Assert.assertTrue;

public class MeasureProcessorEvaluatedResourceTest extends BaseMeasureProcessorTest {

    public MeasureProcessorEvaluatedResourceTest() {
        super("ContentBundleUpdated.json");
    }

    @Test
    public void measure_eval_contained_is_unique() {
        MeasureReport report = this.measureProcessor.evaluateMeasure(
                "http://cds.optum.com/dqm-hypertension/fhir/Measure/HTN1Measure",
                "2020-08-16", "2022-08-16", "subject",
                "Patient/HTN1-patient-1", null, null,
                endpoint, endpoint, endpoint, null);

        assertNotNull(report);

        //check contained duplicates
        HashSet<String> containedIdSet = new HashSet<>();
        AtomicInteger count = new AtomicInteger();
        report.getContained().forEach(x -> {containedIdSet.add(x.getId()); count.getAndIncrement();});
        assertEquals(count.get(), containedIdSet.size());

        //check evaluated resource
        assertEquals( report.getEvaluatedResource().size(), 8);

        assertTrue(report.getEvaluatedResource().stream().anyMatch(
                item -> item.getReference().equalsIgnoreCase("Encounter/HTN1-patient-1-encounter-2") &&
                        ((StringType)item.getExtension().get(0).getValue()).getValue().equalsIgnoreCase("initial-population")));

        assertTrue(report.getEvaluatedResource().stream().anyMatch(
                item -> item.getReference().equalsIgnoreCase("Observation/HTN1-patient-1-observation-3") &&
                        ((StringType)item.getExtension().get(0).getValue()).getValue().equalsIgnoreCase("numerator")));
    }
}