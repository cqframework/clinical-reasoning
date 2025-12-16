package org.opencds.cqf.fhir.cr.measure.dstu3.selected.def;

import static org.junit.jupiter.api.Assertions.*;

import org.opencds.cqf.fhir.cr.measure.common.def.report.GroupReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.PopulationReportDef;

/**
 * Fluent API for asserting on GroupDef (pre-scoring group state) from DSTU3 measure evaluation.
 *
 * @param <P> parent type for up() navigation
 */
public class SelectedMeasureDefGroup<P> {
    protected final GroupReportDef groupDef;
    protected final P parent;

    public SelectedMeasureDefGroup(GroupReportDef groupDef, P parent) {
        this.groupDef = groupDef;
        this.parent = parent;
    }

    public GroupReportDef value() {
        return groupDef;
    }

    public P up() {
        return parent;
    }

    // Access population by name
    public SelectedMeasureDefPopulation<SelectedMeasureDefGroup<P>> population(String populationName) {
        PopulationReportDef found = groupDef.populations().stream()
                .filter(pop -> pop.code() != null
                        && !pop.code().isEmpty()
                        && pop.code().first().code().equals(populationName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Population '" + populationName
                        + "' not found in group. Available: "
                        + groupDef.populations().stream()
                                .map(pop -> pop.code() != null && !pop.code().isEmpty()
                                        ? pop.code().first().code()
                                        : "null")
                                .toList()));
        return new SelectedMeasureDefPopulation<>(found, this);
    }

    // Access first population
    public SelectedMeasureDefPopulation<SelectedMeasureDefGroup<P>> firstPopulation() {
        assertFalse(groupDef.populations().isEmpty(), "Expected at least one population but found none");
        return new SelectedMeasureDefPopulation<>(groupDef.populations().get(0), this);
    }

    // Access population by index
    public SelectedMeasureDefPopulation<SelectedMeasureDefGroup<P>> population(int index) {
        assertTrue(
                index >= 0 && index < groupDef.populations().size(),
                "Index " + index + " out of bounds for "
                        + groupDef.populations().size() + " populations");
        return new SelectedMeasureDefPopulation<>(groupDef.populations().get(index), this);
    }

    // Assert stratifier count
    public SelectedMeasureDefGroup<P> hasStratifierCount(int expected) {
        assertEquals(
                expected,
                groupDef.stratifiers().size(),
                "Expected " + expected + " stratifiers but found "
                        + groupDef.stratifiers().size());
        return this;
    }

    // Assert population count
    public SelectedMeasureDefGroup<P> hasPopulationCount(int expected) {
        assertEquals(
                expected,
                groupDef.populations().size(),
                "Expected " + expected + " populations but found "
                        + groupDef.populations().size());
        return this;
    }

    // Assert null score (pre-scoring)
    public SelectedMeasureDefGroup<P> hasNullScore() {
        assertNull(groupDef.getScore(), "Expected null score (pre-scoring) but found: " + groupDef.getScore());
        return this;
    }
}
