package org.opencds.cqf.cql.evaluator.measure.r4;

import java.math.BigDecimal;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


@Test(singleThreaded = true)
public class SimpleMeasureProcessorTest extends BaseMeasureProcessorTest {
    
    public SimpleMeasureProcessorTest() {
        super("EXM108-8.3.000-bundle.json");
    }

    @Test
    public void exm108_partialSubjectId() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "subject", "numer-EXM108", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);

        report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "subject", "denom-EXM108", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 0);
        validateGroup(report.getGroup().get(0), "denominator", 1);
    }

    @Test
    public void exm108_fullSubjectId() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "subject", "Patient/numer-EXM108", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);
        validateGroupScore(report.getGroup().get(0), new BigDecimal("1.0"));
    }

    @Test
    public void exm108_population() {
        // This bundle has two patients, a numerator and denominator
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "population", null, null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 2);
        validateGroup(report.getGroup().get(0), "initial-population", 2);
        validateGroupScore(report.getGroup().get(0), new BigDecimal("0.5"));

        assertEquals(report.getType(), MeasureReportType.SUMMARY);
    }

    @Test
    public void exm108_noReportType_noSubject_runsPopulation() {
        // This default behavior if no type or subject is specified is "population"
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", null, null, null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 2);
        validateGroup(report.getGroup().get(0), "initial-population", 2);
        validateGroupScore(report.getGroup().get(0), new BigDecimal("0.5"));

        assertEquals(report.getType(), MeasureReportType.SUMMARY);
    }


    @Test
    public void exm108_noType_hasSubject_runsIndividual() {
        // This default behavior if no type is specified is "individual"
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", null, "Patient/numer-EXM108", null, null, endpoint, endpoint, endpoint, null);
        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);
        validateGroupScore(report.getGroup().get(0), new BigDecimal("1.0"));


        assertEquals(report.getType(), MeasureReportType.INDIVIDUAL);
    }


    @Test
    public void exm108_singlePatient_hasMetadata() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "subject", "Patient/numer-EXM108", null, null, endpoint, endpoint, endpoint, null);

        assertEquals(report.getType(), MeasureReportType.INDIVIDUAL);
        assertEquals(report.getMeasure(), "http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108");
        assertEquals(report.getSubject().getReference(), "Patient/numer-EXM108");

        // TODO: The MeasureProcessor assumes local timezone if none is specified.
        // Need to make the test smart enough to handle that.
        assertEquals(report.getPeriod().getStartElement().getYear(), (Integer)2018);
        // assertEquals(report.getPeriod().getStartElement().getMonth(), (Integer)12);
        assertEquals(report.getPeriod().getStartElement().getDay(), (Integer)31);
     
        assertEquals(report.getPeriod().getEndElement().getYear(), (Integer)2019);
        // assertEquals(report.getPeriod().getEndElement().getMonth(), (Integer)12);
        assertEquals(report.getPeriod().getEndElement().getDay(), (Integer)31);

        // TODO: Should be the evaluation date. Or approximately "now"
        assertNotNull(report.getDate());
    }

    // @Test
    // public void exm108_viewOutput() {
    //     MeasureReport report = 
    //     this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM108", "2018-12-31", "2019-12-31", "population", null, null, null, endpoint, endpoint, endpoint, null);
    //     ca.uhn.fhir.parser.IParser parser = fhirContext.newJsonParser();
    //     parser.setPrettyPrint(true);
    //     String result = parser.encodeResourceToString(report);
    //     System.out.println(result);
    // }
}
