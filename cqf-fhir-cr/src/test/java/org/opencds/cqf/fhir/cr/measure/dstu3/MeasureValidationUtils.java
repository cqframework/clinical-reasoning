package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.MeasureReport;

public class MeasureValidationUtils {

    protected static void validateGroupScore(MeasureReport.MeasureReportGroupComponent group, String score) {
        validateGroupScore(group, new BigDecimal(score));
    }

    protected static void validateGroupScore(MeasureReport.MeasureReportGroupComponent group, BigDecimal score) {
        assertTrue(group.hasMeasureScore(), "group \"%s\" does not have a score".formatted(group.getId()));
        assertEquals(group.getMeasureScore(), score);
    }

    protected static void validateGroup(
            MeasureReport.MeasureReportGroupComponent group, String populationName, int count) {
        Optional<MeasureReport.MeasureReportGroupPopulationComponent> population = group.getPopulation().stream()
                .filter(x -> x.hasCode()
                        && x.getCode().hasCoding()
                        && x.getCode().getCoding().get(0).getCode().equals(populationName))
                .findFirst();

        assertTrue(population.isPresent(), "Unable to locate a population with id \"%s\"".formatted(populationName));

        validatePopulation(population.get(), count);
    }

    protected static void validatePopulation(
            MeasureReport.MeasureReportGroupPopulationComponent population, int count) {
        assertEquals(
                count,
                population.getCount(),
                "expected count for population \"%s\" did not match".formatted(population.getId()));
    }

    protected static void validateStratifier(
            MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent,
            String stratumValue,
            String populationName,
            int count) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().equals(stratumValue))
                .findFirst();
        assertTrue(stratumOpt.isPresent(), "Group does not have a stratum with value: \"%s\"".formatted(stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        Optional<MeasureReport.StratifierGroupPopulationComponent> population = stratum.getPopulation().stream()
                .filter(x -> x.hasCode()
                        && x.getCode().hasCoding()
                        && x.getCode().getCoding().get(0).getCode().equals(populationName))
                .findFirst();

        assertTrue(population.isPresent(), "Unable to locate a population with id \"%s\"".formatted(populationName));

        assertEquals(
                population.get().getCount(),
                count,
                "expected count for stratum value \"%s\" population \"%s\" did not match"
                        .formatted(stratumValue, populationName));
    }

    protected static void validateStratumScore(
            MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent,
            String stratumValue,
            BigDecimal score) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream()
                .filter(x -> x.hasValue() && x.getValue().equals(stratumValue))
                .findFirst();
        assertTrue(stratumOpt.isPresent(), "Group does not have a stratum with value: \"%s\"".formatted(stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        assertTrue(stratum.hasMeasureScore(), "stratum \"%s\" does not have a score".formatted(stratum.getId()));
        assertEquals(stratum.getMeasureScore(), score);
    }

    public static void validateListEquality(ListResource actual, ListResource expected) {
        Set<String> listItems = new HashSet<>();

        for (ListResource.ListEntryComponent comp : actual.getEntry()) {
            if (comp.hasItem()) {
                listItems.add(comp.getItem().getReference());
            }
        }

        for (ListResource.ListEntryComponent comp : expected.getEntry()) {
            assertTrue(actual.getEntry().stream()
                    .anyMatch(x -> listItems.contains(comp.getItem().getReference())));
        }
    }
}
