package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selector;
import org.opencds.cqf.fhir.cr.measure.r4.MeasureValidationUtils;

public class SelectedMeasureReportStratum
        extends Selected<MeasureReport.StratifierGroupComponent, SelectedMeasureReportStratifier> {

    public SelectedMeasureReportStratum(
            MeasureReport.StratifierGroupComponent value, SelectedMeasureReportStratifier parent) {
        super(value, parent);
    }

    public SelectedMeasureReportStratum hasScore(String score) {
        MeasureValidationUtils.validateStratumScore(value(), score);
        return this;
    }

    public SelectedMeasureReportStratum hasComponentStratifierCount(int count) {
        assertEquals(count, value().getComponent().size());
        return this;
    }

    public SelectedMeasureReportStratum hasPopulationCount(int count) {
        final StratifierGroupComponent value = this.value();
        final List<StratifierGroupPopulationComponent> population = value.getPopulation();
        assertEquals(count, population.size());
        return this;
    }

    public SelectedMeasureReportStratumPopulation firstPopulation() {
        return population(MeasureReport.StratifierGroupComponent::getPopulationFirstRep);
    }

    public SelectedMeasureReportStratum hasValue(String textValue) {
        assertTrue(value().hasValue() && value().getValue().hasText());
        assertEquals(textValue, value().getValue().getText());
        return this;
    }

    public SelectedMeasureReportStratumPopulation population(String name) {
        var population = population(s -> s.getPopulation().stream()
                .filter(x -> x.hasCode()
                        && x.getCode().hasCoding()
                        && x.getCode().getCoding().get(0).getCode().equals(name))
                .findFirst()
                .orElse(null));

        assertNotNull(population);

        return population;
    }

    public SelectedMeasureReportStratumPopulation populationId(String name) {
        var population = population(s -> s.getPopulation().stream()
                .filter(x -> x.getId().equals(name))
                .findFirst()
                .orElse(null));

        assertNotNull(population);

        return population;
    }

    public SelectedMeasureReportStratumPopulation population(
            Selector<MeasureReport.StratifierGroupPopulationComponent, MeasureReport.StratifierGroupComponent>
                    populationSelector) {
        if (populationSelector == null) {
            return null;
        }
        if (value() == null) {
            return null;
        }

        var p = populationSelector.select(value());
        if (p == null) {
            return null;
        }
        return new SelectedMeasureReportStratumPopulation(p, this);
    }
}
