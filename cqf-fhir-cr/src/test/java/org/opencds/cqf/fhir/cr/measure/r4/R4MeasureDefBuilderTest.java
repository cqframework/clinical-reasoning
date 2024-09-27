package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.POPULATION_BASIS_URL;

import java.util.Collections;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class R4MeasureDefBuilderTest {

    private static final R4MeasureDefBuilder BUILDER = new R4MeasureDefBuilder();

    private static final Extension BOOLEAN_BASIS_EXT =
            new Extension().setUrl(POPULATION_BASIS_URL).setValue(new StringType("boolean"));
    private static final Extension RESOURCE_BASIS_EXT =
            new Extension().setUrl(POPULATION_BASIS_URL).setValue(new StringType("encounter"));

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

    @Test
    void defBuilderBooleanBasis_WithResourceExt() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        measure.addGroup().addPopulation().setId("pop-id");
        measure.setExtension(Collections.singletonList(RESOURCE_BASIS_EXT));

        var def = BUILDER.build(measure);
        Assertions.assertFalse(def.isBooleanBasis());
    }

    @Test
    void defBuilderBooleanBasis_WithBooleanExt() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        measure.addGroup().addPopulation().setId("pop-id");
        measure.setExtension(Collections.singletonList(BOOLEAN_BASIS_EXT));

        var def = BUILDER.build(measure);
        Assertions.assertTrue(def.isBooleanBasis());
    }

    @Test
    void defBuilderBooleanBasis_NoBasisExt() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        measure.addGroup().addPopulation().setId("pop-id");

        var def = BUILDER.build(measure);
        Assertions.assertTrue(def.isBooleanBasis());
    }
}
