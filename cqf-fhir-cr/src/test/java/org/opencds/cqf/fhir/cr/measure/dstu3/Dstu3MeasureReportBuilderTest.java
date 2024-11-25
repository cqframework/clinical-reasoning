package org.opencds.cqf.fhir.cr.measure.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.Measure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class Dstu3MeasureReportBuilderTest {

    protected Dstu3MeasureReportBuilder measureReportBuilder;
    protected FhirContext fhirContext;

    @BeforeAll
    void setup() {
        this.measureReportBuilder = new Dstu3MeasureReportBuilder();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
    }

    @Test
    void checkIfNotBooleanBasedMeasure() {
        Dstu3MeasureDefBuilder defBuilder = new Dstu3MeasureDefBuilder();
        IParser parser = fhirContext.newJsonParser();

        Measure measureEncounterBias = (Measure)
                parser.parseResource(Dstu3MeasureReportBuilderTest.class.getResourceAsStream("EXM105FHIR3Sample.json"));
        var measureEBDef = defBuilder.build(measureEncounterBias);

        Measure measureWithoutExtension = (Measure) parser.parseResource(
                Dstu3MeasureReportBuilderTest.class.getResourceAsStream("EXM105FHIR3SampleWithoutExtension.json"));
        var measureWEDef = defBuilder.build(measureWithoutExtension);

        Measure measureBooleanBias =
                (Measure) parser.parseResource(Dstu3MeasureReportBuilderTest.class.getResourceAsStream(
                        "EXM105FHIR3SampleWithBoolenPopulationBias.json"));
        var measureBBDef = defBuilder.build(measureBooleanBias);

        // Encounter Basis
        /* assertNotNull(measureEncounterBias);
        assertFalse(measureEBDef.isBooleanBasis());
        // No specified Basis
        assertNotNull(measureWithoutExtension);
        assertTrue(measureWEDef.isBooleanBasis());
        // Boolean Basis
        assertNotNull(measureBooleanBias);
        assertTrue(measureBBDef.isBooleanBasis());*/
    }
}
