package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;

class R4MeasureReportUtilsTest {

    @Test
    void testGetCountFromGroupPopulation_Found() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(10);
        populations.add(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(20);
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulation(populations, "numerator");

        assertEquals(10, count);
    }

    @Test
    void testGetCountFromGroupPopulation_NotFound() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(10);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulation(populations, "denominator-exclusion");

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromGroupPopulation_ByType() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(15);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulation(populations, MeasurePopulationType.NUMERATOR);

        assertEquals(15, count);
    }

    @Test
    void testGetCountFromGroupPopulation_FromGroupComponent() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(25);
        group.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulation(group, MeasurePopulationType.DENOMINATOR);

        assertEquals(25, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_Found() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(5);
        populations.add(numerator);

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(8);
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromStratifierPopulation(populations, "denominator");

        assertEquals(8, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_NotFound() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(5);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromStratifierPopulation(populations, "denominator");

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_ByType() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(7);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromStratifierPopulation(populations, MeasurePopulationType.NUMERATOR);

        assertEquals(7, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_FromStratum() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(12);
        stratum.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromStratifierPopulation(stratum, MeasurePopulationType.DENOMINATOR);

        assertEquals(12, count);
    }

    @Test
    void testGetPopulationTypes() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        group.addPopulation(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        group.addPopulation(denominator);

        MeasureReportGroupPopulationComponent denominatorExclusion = new MeasureReportGroupPopulationComponent();
        denominatorExclusion.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator-exclusion")));
        group.addPopulation(denominatorExclusion);

        Set<MeasurePopulationType> types = R4MeasureReportUtils.getPopulationTypes(group);

        assertNotNull(types);
        assertEquals(3, types.size());
        assertTrue(types.contains(MeasurePopulationType.NUMERATOR));
        assertTrue(types.contains(MeasurePopulationType.DENOMINATOR));
        assertTrue(types.contains(MeasurePopulationType.DENOMINATOREXCLUSION));
    }

    @Test
    void testDoesPopulationTypeMatch_GroupPopulation_True() {
        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(numerator, MeasurePopulationType.NUMERATOR);

        assertTrue(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_GroupPopulation_False() {
        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(numerator, MeasurePopulationType.DENOMINATOR);

        assertFalse(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_StratifierPopulation_True() {
        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(denominator, MeasurePopulationType.DENOMINATOR);

        assertTrue(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_StratifierPopulation_False() {
        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(denominator, MeasurePopulationType.NUMERATOR);

        assertFalse(matches);
    }

    // Note: testGetStratumDefText and testMatchesStratumValue tests omitted
    // These methods operate on complex internal data structures (StratumDef, StratifierDef, etc.)
    // that are tested through integration tests in the measure evaluation suite.
    // The utility methods themselves are extracted from proven working code in
    // R4MeasureReportScorer and R4MeasureReportBuilder.
}
