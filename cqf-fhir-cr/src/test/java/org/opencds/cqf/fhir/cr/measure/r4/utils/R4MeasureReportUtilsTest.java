package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOR);

        assertEquals(10, count);
    }

    @Test
    void testGetCountFromGroupPopulation_NotFound() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(10);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOREXCLUSION);

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromGroupPopulation_ByType() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(15);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOR);

        assertEquals(15, count);
    }

    @Test
    void testGetCountFromGroupPopulation_FromGroupComponent() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(25);
        group.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                group, MeasurePopulationType.DENOMINATOR);

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

        int count = R4MeasureReportUtils.getCountFromStratumPopulationByType(
                populations, MeasurePopulationType.DENOMINATOR);

        assertEquals(8, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_NotFound() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(5);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationByType(
                populations, MeasurePopulationType.DENOMINATOR);

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_ByType() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(7);
        populations.add(numerator);

        int count =
                R4MeasureReportUtils.getCountFromStratumPopulationByType(populations, MeasurePopulationType.NUMERATOR);

        assertEquals(7, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_FromStratum() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(12);
        stratum.addPopulation(denominator);

        int count =
                R4MeasureReportUtils.getCountFromStratumPopulationByType(stratum, MeasurePopulationType.DENOMINATOR);

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

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationType - Failure Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationType_MultiplePopulationsWithSameType_ThrowsException() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        // Add two numerator populations - this is invalid
        MeasureReportGroupPopulationComponent numerator1 = new MeasureReportGroupPopulationComponent();
        numerator1.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator1.setCount(10);
        numerator1.setId("numerator-1");
        populations.add(numerator1);

        MeasureReportGroupPopulationComponent numerator2 = new MeasureReportGroupPopulationComponent();
        numerator2.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator2.setCount(20);
        numerator2.setId("numerator-2");
        populations.add(numerator2);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                        populations, MeasurePopulationType.NUMERATOR));

        assertTrue(exception.getMessage().contains("Expected only a single population"));
        assertTrue(exception.getMessage().contains("NUMERATOR"));
    }

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationId - Happy Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationId_Found() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(15);
        numerator.setId("numerator-1");
        populations.add(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(30);
        denominator.setId("denominator-1");
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "denominator-1");

        assertEquals(30, count);
    }

    @Test
    void testGetCountFromGroupPopulationByPopulationId_NotFound() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(15);
        numerator.setId("numerator-1");
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "missing-id");

        assertEquals(0, count);
    }

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationId - Failure Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationId_MultiplePopulationsWithSameId_ThrowsException() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        // Add two populations with the same ID - this is invalid
        MeasureReportGroupPopulationComponent pop1 = new MeasureReportGroupPopulationComponent();
        pop1.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        pop1.setCount(10);
        pop1.setId("duplicate-id");
        populations.add(pop1);

        MeasureReportGroupPopulationComponent pop2 = new MeasureReportGroupPopulationComponent();
        pop2.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        pop2.setCount(20);
        pop2.setId("duplicate-id");
        populations.add(pop2);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "duplicate-id"));

        assertTrue(exception.getMessage().contains("Expected only a single population"));
        assertTrue(exception.getMessage().contains("duplicate-id"));
    }

    // ========================================
    // Tests for getCountFromGroupPopulationById (convenience method)
    // ========================================

    @Test
    void testGetCountFromGroupPopulationById_FromGroupComponent() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(42);
        numerator.setId("numerator-1");
        group.addPopulation(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(100);
        denominator.setId("denominator-1");
        group.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationById(group, "numerator-1");

        assertEquals(42, count);
    }

    @Test
    void testGetCountFromGroupPopulationById_NotFound() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(42);
        numerator.setId("numerator-1");
        group.addPopulation(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationById(group, "missing-id");

        assertEquals(0, count);
    }

    // ========================================
    // Tests for getCountFromStratumPopulationByType - Failure Path
    // ========================================

    @Test
    void testGetCountFromStratumPopulationByType_MultiplePopulationsWithSameType_ThrowsException() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        // Add two numerator populations - this is invalid
        StratifierGroupPopulationComponent numerator1 = new StratifierGroupPopulationComponent();
        numerator1.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator1.setCount(5);
        populations.add(numerator1);

        StratifierGroupPopulationComponent numerator2 = new StratifierGroupPopulationComponent();
        numerator2.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator2.setCount(7);
        populations.add(numerator2);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getCountFromStratumPopulationByType(
                        populations, MeasurePopulationType.NUMERATOR));

        assertTrue(exception.getMessage().contains("Got back more than one stratum population"));
        assertTrue(exception.getMessage().contains("NUMERATOR"));
    }

    // ========================================
    // Tests for getCountFromStratumPopulationById - Happy Path
    // ========================================

    @Test
    void testGetCountFromStratumPopulationById_FromList_Found() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(8);
        numerator.setId("num-1");
        populations.add(numerator);

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(16);
        denominator.setId("den-1");
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(populations, "den-1");

        assertEquals(16, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromList_NotFound() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(8);
        numerator.setId("num-1");
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(populations, "missing-id");

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromStratum_Found() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(25);
        numerator.setId("num-1");
        stratum.addPopulation(numerator);

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(50);
        denominator.setId("den-1");
        stratum.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(stratum, "num-1");

        assertEquals(25, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromStratum_NotFound() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(25);
        numerator.setId("num-1");
        stratum.addPopulation(numerator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(stratum, "missing-id");

        assertEquals(0, count);
    }

    // Note: testGetStratumDefText and testMatchesStratumValue tests omitted
    // These methods operate on complex internal data structures (StratumDef, StratifierDef, etc.)
    // that are tested through integration tests in the measure evaluation suite.
    // The utility methods themselves are extracted from proven working code in
    // R4MeasureReportScorer and R4MeasureReportBuilder.
}
