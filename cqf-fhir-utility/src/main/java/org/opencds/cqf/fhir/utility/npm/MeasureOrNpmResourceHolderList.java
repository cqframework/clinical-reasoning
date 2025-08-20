package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.Measure;

// LUKETODO:  top level record
public final class MeasureOrNpmResourceHolderList {

    private final List<MeasureOrNpmResourceHolder> measuresPlusNpmResourceHolders;

    public static MeasureOrNpmResourceHolderList of(MeasureOrNpmResourceHolder measureOrNpmResourceHolder) {
        return new MeasureOrNpmResourceHolderList(List.of(measureOrNpmResourceHolder));
    }

    public static MeasureOrNpmResourceHolderList of(List<MeasureOrNpmResourceHolder> measureOrNpmResourceHolders) {
        return new MeasureOrNpmResourceHolderList(measureOrNpmResourceHolders);
    }

    public static MeasureOrNpmResourceHolderList of(Measure measure) {
        return new MeasureOrNpmResourceHolderList(List.of(MeasureOrNpmResourceHolder.measureOnly(measure)));
    }

    public static MeasureOrNpmResourceHolderList ofMeasures(List<Measure> measures) {
        return new MeasureOrNpmResourceHolderList(
                measures.stream().map(MeasureOrNpmResourceHolder::measureOnly).toList());
    }

    private MeasureOrNpmResourceHolderList(List<MeasureOrNpmResourceHolder> measuresPlusNpmResourceHolders) {
        this.measuresPlusNpmResourceHolders = measuresPlusNpmResourceHolders;
    }

    public List<MeasureOrNpmResourceHolder> getMeasuresOrNpmResourceHolders() {
        return measuresPlusNpmResourceHolders;
    }

    List<Measure> measures() {
        return this.measuresPlusNpmResourceHolders.stream()
                .map(MeasureOrNpmResourceHolder::getMeasure)
                .toList();
    }

    public List<NpmResourceHolder> npmResourceHolders() {
        return this.measuresPlusNpmResourceHolders.stream()
                .map(MeasureOrNpmResourceHolder::npmResourceHolder)
                .toList();
    }

    public List<Measure> getMeasures() {
        return measuresPlusNpmResourceHolders.stream()
                .map(MeasureOrNpmResourceHolder::getMeasure)
                .toList();
    }

    public void checkMeasureLibraries() {
        for (MeasureOrNpmResourceHolder measureOrNpmResourceHolder : measuresPlusNpmResourceHolders) {
            if (!measureOrNpmResourceHolder.hasLibrary()) {
                throw new InvalidRequestException("Measure %s does not have a primary library specified"
                        .formatted(measureOrNpmResourceHolder.getMeasureUrl()));
            }
        }
    }

    public int size() {
        return measuresPlusNpmResourceHolders.size();
    }

    public List<MeasureOrNpmResourceHolder> measuresPlusNpmResourceHolders() {
        return measuresPlusNpmResourceHolders;
    }

    public List<String> getMeasureUrls() {
        return this.measuresPlusNpmResourceHolders.stream()
                .map(MeasureOrNpmResourceHolder::getMeasureUrl)
                .toList();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (MeasureOrNpmResourceHolderList) obj;
        return Objects.equals(this.measuresPlusNpmResourceHolders, that.measuresPlusNpmResourceHolders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(measuresPlusNpmResourceHolders);
    }

    @Override
    public String toString() {
        return "MeasurePlusNpmResourceHolderList[" + "measuresPlusNpmResourceHolders=" + measuresPlusNpmResourceHolders
                + ']';
    }
}
