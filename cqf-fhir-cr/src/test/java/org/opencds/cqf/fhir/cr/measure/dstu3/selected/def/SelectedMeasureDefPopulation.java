package org.opencds.cqf.fhir.cr.measure.dstu3.selected.def;

import static org.junit.jupiter.api.Assertions.*;

import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;

/**
 * Fluent API for asserting on PopulationDef (pre-scoring population state) from DSTU3 measure evaluation.
 *
 * @param <P> parent type for up() navigation
 */
public class SelectedMeasureDefPopulation<P> {
    protected final PopulationDef populationDef;
    protected final MeasureEvaluationState state;
    protected final P parent;

    public SelectedMeasureDefPopulation(PopulationDef populationDef, MeasureEvaluationState state, P parent) {
        this.populationDef = populationDef;
        this.state = state;
        this.parent = parent;
    }

    public PopulationDef value() {
        return populationDef;
    }

    public P up() {
        return parent;
    }

    // Assert population count (delegates to PopulationState.getCount())
    public SelectedMeasureDefPopulation<P> hasCount(int expected) {
        int actualCount = state.population(populationDef).getCount();
        assertEquals(expected, actualCount, "Population count mismatch");
        return this;
    }

    // Assert subject count
    public SelectedMeasureDefPopulation<P> hasSubjectCount(int expected) {
        assertEquals(
                expected,
                state.population(populationDef).getSubjectResources().size(),
                "Expected " + expected + " subjects but found "
                        + state.population(populationDef).getSubjectResources().size());
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
                state.population(populationDef).getSubjectResources().isEmpty(),
                "Expected no subjects but found "
                        + state.population(populationDef).getSubjectResources().size());
        return this;
    }

    // Assert has subjects
    public SelectedMeasureDefPopulation<P> hasSubjects() {
        assertFalse(
                state.population(populationDef).getSubjectResources().isEmpty(), "Expected subjects but found none");
        return this;
    }
}
