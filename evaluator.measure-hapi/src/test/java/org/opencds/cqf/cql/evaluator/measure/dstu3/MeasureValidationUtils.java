package org.opencds.cqf.cql.evaluator.measure.dstu3;

import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MeasureValidationUtils {

    protected static void validateGroupScore(MeasureReport.MeasureReportGroupComponent group, BigDecimal score) {
        assertTrue(group.hasMeasureScore(), String.format("group \"%s\" does not have a score", group.getId()));
        assertEquals(group.getMeasureScore(), score);
    }
    public static void validateGroup(MeasureReport.MeasureReportGroupComponent group, String populationName, int count) {
        Optional<MeasureReport.MeasureReportGroupPopulationComponent> population = group.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding() && x.getCode().getCoding().get(0).getCode().equals(populationName)).findFirst();

        assertTrue(population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));
        assertEquals(population.get().getCount(), count, String.format("expected count for population \"%s\" did not match", populationName));
    }

    protected static void validateStratifier(MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent, String stratumValue, String populationName, int count) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream().filter(x -> x.hasValue() && x.getValue().equals(stratumValue)).findFirst();
        assertTrue(stratumOpt.isPresent(), String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        Optional<MeasureReport.StratifierGroupPopulationComponent> population = stratum.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding() && x.getCode().getCoding().get(0).getCode().equals(populationName)).findFirst();

        assertTrue(population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));

        assertEquals(population.get().getCount(), count, String.format("expected count for stratum value \"%s\" population \"%s\" did not match", stratumValue, populationName));
    }

    protected static void validateStratumScore(MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent, String stratumValue, BigDecimal score) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream().filter(x -> x.hasValue() && x.getValue().equals(stratumValue)).findFirst();
        assertTrue(stratumOpt.isPresent(), String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        assertTrue(stratum.hasMeasureScore(), String.format("stratum \"%s\" does not have a score", stratum.getId()));
        assertEquals(stratum.getMeasureScore(), score);
    }


    public static void validateListEquality(ListResource actual, ListResource expected) {
        Set<String> listItems = new HashSet<>();

        for (ListResource.ListEntryComponent comp : actual.getEntry()) {
            if (comp.hasItem()) {
                listItems.add(comp.getItem().getReference());
            }
        }

        for(ListResource.ListEntryComponent comp : expected.getEntry()) {
            assertTrue(actual.getEntry().stream().anyMatch(x -> listItems.contains(x.getItem().getReference())));
        }
    }

    public static void validateMeasureReportContained(MeasureReport actual, MeasureReport expected) {
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
