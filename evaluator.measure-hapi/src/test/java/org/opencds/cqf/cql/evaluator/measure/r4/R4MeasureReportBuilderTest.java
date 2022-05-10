package org.opencds.cqf.cql.evaluator.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Measure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class R4MeasureReportBuilderTest {

    protected R4MeasureReportBuilder measureReportBuilder;
    protected FhirContext fhirContext;

    @BeforeClass
    public void setup()
    {
        this.measureReportBuilder = new R4MeasureReportBuilder();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    }

    @Test
    public void checkIfNotBooleanBasedMeasure() {
        IParser parser = fhirContext.newJsonParser();
        Measure measureEncounterBias = (Measure)parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithPopulationBiasEncounter.json"));
        Measure measureWithoutExtension = (Measure)parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithoutExtension.json"));
        Measure measureBooleanBias = (Measure)parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithPopulationBiasBoolean.json"));
        Measure measureWithEmptyExtension = (Measure)parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithEmptyExtension.json"));

        assertNotNull(measureEncounterBias);
        assertTrue(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureEncounterBias));

        assertNotNull(measureWithoutExtension);
        assertFalse(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureWithoutExtension));

        assertNotNull(measureBooleanBias);
        assertFalse(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureBooleanBias));

        assertNotNull(measureWithEmptyExtension);
        assertFalse(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureWithEmptyExtension));

    }
}
