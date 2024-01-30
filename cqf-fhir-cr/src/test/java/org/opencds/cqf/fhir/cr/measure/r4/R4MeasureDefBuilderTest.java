package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;

class R4MeasureDefBuilderTest {

    private static final R4MeasureDefBuilder BUILDER = new R4MeasureDefBuilder();

    @Test
    void defBuilderThrowsWhenMeasureIdMissing() {
        var measure = new Measure();
        var e = assertThrows(NullPointerException.class, () -> BUILDER.build(measure));
        assertTrue(e.getMessage().contains("id is required on all Resources"));
    }

    @Test
    void defBuilderThrowsWhenPopulationIdMissing() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        measure.addGroup().addPopulation();
        var e = assertThrows(NullPointerException.class, () -> BUILDER.build(measure));
        assertTrue(e.getMessage().contains("id is required on all Elements"));
    }
}
