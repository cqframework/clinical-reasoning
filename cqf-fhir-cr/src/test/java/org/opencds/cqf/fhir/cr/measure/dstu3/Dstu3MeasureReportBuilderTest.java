package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.dstu3.model.Measure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

@TestInstance(Lifecycle.PER_CLASS)
public class Dstu3MeasureReportBuilderTest {

  protected Dstu3MeasureReportBuilder measureReportBuilder;
  protected FhirContext fhirContext;

  @BeforeAll
  public void setup() {
    this.measureReportBuilder = new Dstu3MeasureReportBuilder();
    this.fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
  }

  @Test
  public void checkIfNotBooleanBasedMeasure() {
    IParser parser = fhirContext.newJsonParser();
    Measure measureEncounterBias = (Measure) parser.parseResource(
        Dstu3MeasureReportBuilderTest.class.getResourceAsStream("EXM105FHIR3Sample.json"));
    Measure measureWithoutExtension =
        (Measure) parser.parseResource(Dstu3MeasureReportBuilderTest.class
            .getResourceAsStream("EXM105FHIR3SampleWithoutExtension.json"));
    Measure measureBooleanBias = (Measure) parser.parseResource(Dstu3MeasureReportBuilderTest.class
        .getResourceAsStream("EXM105FHIR3SampleWithBoolenPopulationBias.json"));

    assertNotNull(measureEncounterBias);
    assertTrue(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureEncounterBias));

    assertNotNull(measureWithoutExtension);
    assertFalse(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureWithoutExtension));

    assertNotNull(measureBooleanBias);
    assertFalse(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureBooleanBias));

  }
}
