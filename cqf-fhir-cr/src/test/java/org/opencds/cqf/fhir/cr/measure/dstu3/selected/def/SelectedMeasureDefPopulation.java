package org.opencds.cqf.fhir.cr.measure.dstu3.selected.def;

import static org.junit.jupiter.api.Assertions.*;

import org.opencds.cqf.fhir.cr.measure.common.def.report.PopulationReportDef;

/**
 * Fluent API for asserting on PopulationDef (pre-scoring population state) from DSTU3 measure evaluation.
 *
 * @param <P> parent type for up() navigation
 */
public class SelectedMeasureDefPopulation<P> {
    protected final PopulationReportDef populationDef;
    protected final P parent;

    public SelectedMeasureDefPopulation(PopulationReportDef populationDef, P parent) {
        this.populationDef = populationDef;
        this.parent = parent;
    }

    public PopulationReportDef value() {
        return populationDef;
    }

    public P up() {
        return parent;
    }

    // Assert population count (delegates to PopulationDef.getCount())
    public SelectedMeasureDefPopulation<P> hasCount(int expected) {
        int actualCount = populationDef.getCount();
        assertEquals(expected, actualCount, "Population count mismatch");
        return this;
    }

    // Assert subject count
    public SelectedMeasureDefPopulation<P> hasSubjectCount(int expected) {
        assertEquals(
                expected,
                populationDef.getSubjectResources().size(),
                "Expected " + expected + " subjects but found "
                        + populationDef.getSubjectResources().size());
        return this;
    }

    // Assert population code
    public SelectedMeasureDefPopulation<P> hasCode(String expectedCode) {
        assertEquals(expectedCode, populationDef.code(), "Population code mismatch");
        return this;
    }

    // Assert has no subjects
    public SelectedMeasureDefPopulation<P> hasNoSubjects() {
        assertTrue(
                populationDef.getSubjectResources().isEmpty(),
                "Expected no subjects but found "
                        + populationDef.getSubjectResources().size());
        return this;
    }

    // Assert has subjects
    public SelectedMeasureDefPopulation<P> hasSubjects() {
        assertFalse(populationDef.getSubjectResources().isEmpty(), "Expected subjects but found none");
        return this;
    }
}
