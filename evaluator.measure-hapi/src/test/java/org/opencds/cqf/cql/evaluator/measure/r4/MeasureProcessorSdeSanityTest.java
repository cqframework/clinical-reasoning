package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashSet;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.StringType;
import org.testng.annotations.Test;

public class MeasureProcessorSdeSanityTest extends BaseMeasureProcessorTest {
        public MeasureProcessorSdeSanityTest() {
                super("ContentBundleCustom.json");
        }

        @Test
        public void measure_eval_unique_extension_list() {
                MeasureReport report = this.measureProcessor.evaluateMeasure(
                                "http://cds.optum.com/dqm-diabetes/fhir/Measure/DM1Measure",
                                "2020-01-01", "2022-06-29", "subject",
                                "Patient/DM1-patient-1", null, null,
                                endpoint, endpoint, endpoint, null);

                assertNotNull(report);

                HashSet<String> set = new HashSet<>();
                report.getExtension().forEach(x -> set.add(x.getValue().toString()));
                assertEquals(set.size(), report.getExtension().size());

                assertEquals(report.getEvaluatedResource().size(), 10);

                assertTrue(report.getEvaluatedResource().stream().anyMatch(
                                item -> item.getReference().equalsIgnoreCase("Patient/DM1-patient-1") &&
                                                ((StringType) item.getExtension().get(0).getValue()).getValue()
                                                                .equalsIgnoreCase("initial-population")));

                assertTrue(report.getEvaluatedResource().stream().anyMatch(
                                item -> item.getReference().equalsIgnoreCase("Observation/DM1-patient-1-observation-1")
                                                &&
                                                ((StringType) item.getExtension().get(0).getValue()).getValue()
                                                                .equalsIgnoreCase("numerator")));
        }

        @Test
        public void measure_eval_without_measure_period() {
                MeasureReport report = this.measureProcessor.evaluateMeasure(
                        "http://cds.optum.com/dqm-diabetes/fhir/Measure/DM1Measure",
                        null, "", "subject",
                        "Patient/DM1-patient-1", null, null,
                        endpoint, endpoint, endpoint, null);

                assertNotNull(report);

                assertEquals(report.getPeriod().getStart().toString(), "Tue Jan 01 00:00:00 MST 2019");
                assertEquals(report.getPeriod().getEnd().toString(), "Tue Dec 31 23:59:59 MST 2019");
        }
}
