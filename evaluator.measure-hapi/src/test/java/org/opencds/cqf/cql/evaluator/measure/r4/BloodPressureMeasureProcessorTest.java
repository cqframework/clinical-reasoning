package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

import java.io.IOException;


@Test(singleThreaded = true)
public class BloodPressureMeasureProcessorTest extends BaseMeasureProcessorTest {


    // TODO: This Bundle is currently bad. It's missing Patients    
    public BloodPressureMeasureProcessorTest() {
        super("ControllingBloodPressureFHIR-bundle.json");
    }

    @Test(enabled = false)
    public void exm165_singlePatient_numerator() throws IOException {

        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/chronic-ds/Measure/ControllingBloodPressureFHIR", "2019-01-01", "2020-01-01", "patient", "numer-EXM165", null, null, endpoint, endpoint, endpoint, null);
        assertNotNull(report);

        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);

        // java.io.File yourFile = new java.io.File("target/sample.json");
        // yourFile.createNewFile(); // if file already exists will do nothing 

        // ca.uhn.fhir.parser.IParser parser = fhirContext.newJsonParser();
        // parser.setPrettyPrint(true);

        // java.io.FileWriter fileWriter = new java.io.FileWriter(yourFile);
        // fileWriter.write(parser.encodeResourceToString(report));
        // fileWriter.flush();
        // fileWriter.close();


    }

    @Test(enabled = false)
    public void exm165_population() throws IOException {

        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/chronic-ds/Measure/ControllingBloodPressureFHIR", "2019-01-01", "2020-01-01", "population", null, null, null, endpoint, endpoint, endpoint, null);
        assertNotNull(report);

        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 2);
    }
}