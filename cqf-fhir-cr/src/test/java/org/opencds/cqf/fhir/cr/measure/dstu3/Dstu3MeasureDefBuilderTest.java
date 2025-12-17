package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.POPULATION_BASIS_URL;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Collections;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;

class Dstu3MeasureDefBuilderTest {

    private static final Dstu3MeasureDefBuilder BUILDER = new Dstu3MeasureDefBuilder();
    private static final Extension BOOLEAN_BASIS_EXT =
            new Extension().setUrl(POPULATION_BASIS_URL).setValue(new StringType("boolean"));
    private static final Extension RESOURCE_BASIS_EXT =
            new Extension().setUrl(POPULATION_BASIS_URL).setValue(new StringType("encounter"));
    private static final String INCREASE = "increase";
    private static final String DECREASE = "decrease";

    @Test
    void defBuilderThrowsWhenMeasureIdMissing() {
        var measure = new Measure();
        var e = assertThrows(InvalidRequestException.class, () -> BUILDER.build(measure));
        assertTrue(e.getMessage().contains("id is required on all Resources"));
    }

    @Test
    void defBuilderThrowsWhenPopulationIdMissing() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        measure.addGroup().addPopulation();
        var e = assertThrows(InvalidRequestException.class, () -> BUILDER.build(measure));
        assertTrue(e.getMessage().contains("id is required on all Elements"));
    }

    @Test
    void defBuilderBooleanBasis_WithResourceExt() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        var grp = measure.addGroup().addPopulation();
        grp.setId("pop-id");
        grp.getCode().getCodingFirstRep().setCode(MeasurePopulationType.INITIALPOPULATION.toCode());
        measure.setExtension(Collections.singletonList(RESOURCE_BASIS_EXT));

        var def = BUILDER.build(measure);
        assertFalse(def.groups().get(0).isBooleanBasis());
    }

    @Test
    void defBuilderBooleanBasis_WithBooleanExt() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        var grp = measure.addGroup().addPopulation();
        grp.setId("pop-id");
        grp.getCode().getCodingFirstRep().setCode(MeasurePopulationType.INITIALPOPULATION.toCode());
        measure.setExtension(Collections.singletonList(BOOLEAN_BASIS_EXT));

        var def = BUILDER.build(measure);
        assertTrue(def.groups().get(0).isBooleanBasis());
    }

    @Test
    void defBuilderBooleanBasis_NoBasisExt() {
        var measure = new Measure();
        measure.setId("123");
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        var grp = measure.addGroup().addPopulation();
        grp.setId("pop-id");
        grp.getCode().getCodingFirstRep().setCode(MeasurePopulationType.INITIALPOPULATION.toCode());
        // no extension set to define basis of Measure
        var def = BUILDER.build(measure);
        assertTrue(def.groups().get(0).isBooleanBasis());
    }

    @Test
    void defBuilderImpNotationIncrease() {
        var measure = new Measure();
        measure.setId("123");
        measure.setImprovementNotation(INCREASE);
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        var grp = measure.addGroup().addPopulation();
        grp.setId("pop-id");
        grp.getCode().getCodingFirstRep().setCode(MeasurePopulationType.INITIALPOPULATION.toCode());
        // no extension set to define basis of Measure
        var def = BUILDER.build(measure);
        assertFalse(def.groups().get(0).isGroupImprovementNotation());
        assertEquals(INCREASE, def.groups().get(0).getImprovementNotation().code());
        assertTrue(def.groups().get(0).isIncreaseImprovementNotation());
    }

    @Test
    void defBuilderImpNotationDecrease() {
        var measure = new Measure();
        measure.setId("123");
        measure.setImprovementNotation(DECREASE);
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        var grp = measure.addGroup().addPopulation();
        grp.setId("pop-id");
        grp.getCode().getCodingFirstRep().setCode(MeasurePopulationType.INITIALPOPULATION.toCode());
        // no extension set to define basis of Measure
        var def = BUILDER.build(measure);
        assertFalse(def.groups().get(0).isGroupImprovementNotation());
        assertEquals(DECREASE, def.groups().get(0).getImprovementNotation().code());
        assertFalse(def.groups().get(0).isIncreaseImprovementNotation());
    }

    @Test
    void defBuilderMeasureScoring() {
        var measure = new Measure();
        measure.setId("123");
        measure.setImprovementNotation(INCREASE);
        measure.setScoring(new CodeableConcept().addCoding(new Coding().setCode("proportion")));
        var grp = measure.addGroup().addPopulation();
        grp.setId("pop-id");
        grp.getCode().getCodingFirstRep().setCode(MeasurePopulationType.INITIALPOPULATION.toCode());
        // no extension set to define basis of Measure
        var def = BUILDER.build(measure);
        assertEquals(MeasureScoring.PROPORTION, def.groups().get(0).measureScoring());
    }
}
