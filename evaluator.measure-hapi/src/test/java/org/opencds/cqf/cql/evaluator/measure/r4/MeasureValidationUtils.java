package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MeasureValidationUtils {

    protected static void validateGroupScore(MeasureReport.MeasureReportGroupComponent group, BigDecimal score) {
        assertTrue(group.hasMeasureScore(), String.format("group \"%s\" does not have a score", group.getId()));
        assertEquals(group.getMeasureScore().getValue(), score);
    }

    protected static void validateGroup(MeasureReport.MeasureReportGroupComponent group, String populationName, int count) {
        Optional<MeasureReport.MeasureReportGroupPopulationComponent> population = group.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding() && x.getCode().getCoding().get(0).getCode().equals(populationName)).findFirst();

        assertTrue(population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));
        assertEquals(population.get().getCount(), count, String.format("expected count for population \"%s\" did not match", populationName));
    }

    protected static void validateStratifier(MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent, String stratumValue, String populationName, int count) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream().filter(x -> x.hasValue() && x.getValue().hasText() && x.getValue().getText().equals(stratumValue)).findFirst();
        assertTrue(stratumOpt.isPresent(), String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        Optional<MeasureReport.StratifierGroupPopulationComponent> population = stratum.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding() && x.getCode().getCoding().get(0).getCode().equals(populationName)).findFirst();

        assertTrue(population.isPresent(), String.format("Unable to locate a population with id \"%s\"", populationName));

        assertEquals(population.get().getCount(), count, String.format("expected count for stratum value \"%s\" population \"%s\" did not match", stratumValue, populationName));
    }

    protected static void validateStratumScore(MeasureReport.MeasureReportGroupStratifierComponent stratifierComponent, String stratumValue, BigDecimal score) {
        Optional<MeasureReport.StratifierGroupComponent> stratumOpt = stratifierComponent.getStratum().stream().filter(x -> x.hasValue() && x.getValue().hasText() && x.getValue().getText().equals(stratumValue)).findFirst();
        assertTrue(stratumOpt.isPresent(), String.format("Group does not have a stratum with value: \"%s\"", stratumValue));

        MeasureReport.StratifierGroupComponent stratum = stratumOpt.get();
        assertTrue(stratum.hasMeasureScore(), String.format("stratum \"%s\" does not have a score", stratum.getId()));
        assertEquals(stratum.getMeasureScore().getValue(), score);
    }

    protected static void validateEvaluatedResourceExtension(List<Reference> measureReferences, String resourceId, String... populations) {
        List<Reference> resourceReferences = measureReferences.stream().filter(x -> x.getReference().equals(resourceId)).collect(Collectors.toList());
        assertEquals(resourceReferences.size(), 1);

        Reference reference = resourceReferences.get(0);

        List<Extension> extensions = reference.getExtension().stream().filter(x -> x.getUrl().equals("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-populationReference")).collect(Collectors.toList());

        assertEquals(extensions.size(), populations.length);

        for (String p : populations) {
            assertTrue(extensions.stream().anyMatch(x -> x.getValue().toString().equals(p)));
        }

    }

    protected static void validateContained(ListResource actual, ListResource expected) {
        Stream<ListResource.ListEntryComponent> actualStream =  actual.getEntry().stream();

        for(ListResource.ListEntryComponent comp : expected.getEntry()) {
            assertTrue(actualStream.anyMatch(x -> x.getItem().getReference().equals(comp.getItem().getReference())));
        }
    }

    protected static void validateMeasureRepostContained(ListResource actual, ListResource expected) {

    }

}
