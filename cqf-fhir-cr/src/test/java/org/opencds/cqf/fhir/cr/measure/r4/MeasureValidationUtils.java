package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public class MeasureValidationUtils {

    protected static void validateGroupScore(MeasureReport.MeasureReportGroupComponent group, String score) {
        validateGroupScore(group, new BigDecimal(score));
    }

    protected static void validateGroupScore(MeasureReport.MeasureReportGroupComponent group, BigDecimal score) {
        assertTrue(group.hasMeasureScore(), String.format("group \"%s\" does not have a score", group.getId()));
        assertEquals(group.getMeasureScore().getValue(), score);
    }

    protected static void validateGroup(
            MeasureReport.MeasureReportGroupComponent group, String populationName, int count) {
        Optional<MeasureReport.MeasureReportGroupPopulationComponent> population = group.getPopulation().stream()
                .filter(x -> x.hasCode()
                        && x.getCode().hasCoding()
                        && x.getCode().getCoding().get(0).getCode().equals(populationName))
                .findFirst();

        assertTrue(
                population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));

        validatePopulation(population.get(), count);
    }

    protected static void validatePopulation(
            MeasureReport.MeasureReportGroupPopulationComponent population, int count) {
        assertEquals(
                count,
                population.getCount(),
                String.format("expected count for population \"%s\" did not match", population.getId()));
    }

    protected static void validateStratifier(
            MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent,
            String stratumValue,
            String populationName,
            int count) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream()
                .filter(x -> x.hasValue()
                        && x.getValue().hasText()
                        && x.getValue().getText().equals(stratumValue))
                .findFirst();
        assertTrue(
                stratumOpt.isPresent(),
                String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        Optional<MeasureReport.StratifierGroupPopulationComponent> population = stratum.getPopulation().stream()
                .filter(x -> x.hasCode()
                        && x.getCode().hasCoding()
                        && x.getCode().getCoding().get(0).getCode().equals(populationName))
                .findFirst();

        assertTrue(
                population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));

        assertEquals(
                population.get().getCount(),
                count,
                String.format(
                        "expected count for stratum value \"%s\" population \"%s\" did not match",
                        stratumValue, populationName));
    }

    protected static void validateStratumScore(
            MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent,
            String stratumValue,
            BigDecimal score) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream()
                .filter(x -> x.hasValue()
                        && x.getValue().hasText()
                        && x.getValue().getText().equals(stratumValue))
                .findFirst();
        assertTrue(
                stratumOpt.isPresent(),
                String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        validateStratumScore(stratum, score);
    }

    protected static void validateStratumScore(MeasureReport.StratifierGroupComponent stratum, BigDecimal score) {
        assertTrue(stratum.hasMeasureScore(), String.format("stratum \"%s\" does not have a score", stratum.getId()));
        assertEquals(stratum.getMeasureScore().getValue(), score);
    }

    protected static void validateStratumScore(MeasureReport.StratifierGroupComponent stratum, String score) {
        validateStratumScore(stratum, new BigDecimal(score));
    }

    protected static void validateEvaluatedResourceExtension(
            List<Reference> measureReferences, String resourceId, String... populations) {
        List<Reference> resourceReferences = measureReferences.stream()
                .filter(x -> x.getReference().equals(resourceId))
                .collect(Collectors.toList());
        assertEquals(resourceReferences.size(), 1);
    }

    protected static void validateListEquality(ListResource actual, ListResource expected) {
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

    protected static void validateMeasureReportContained(MeasureReport actual, MeasureReport expected) {
        assertEquals(actual.getContained().size(), expected.getContained().size());

        Map<String, Resource> listResources = new HashMap<>();

        for (Resource resource : actual.getContained()) {
            if (resource.hasId()) {
                listResources.put(resource.getId(), resource);
            }
        }

        for (Resource resource : expected.getContained()) {
            if (resource.hasId() && listResources.containsKey(resource.getId())) {
                if (resource.getResourceType().equals(ResourceType.List)) {
                    validateListEquality((ListResource) listResources.get(resource.getId()), (ListResource) resource);
                    validateListEquality((ListResource) resource, (ListResource) listResources.get(resource.getId()));
                }
            }
        }
    }
}
