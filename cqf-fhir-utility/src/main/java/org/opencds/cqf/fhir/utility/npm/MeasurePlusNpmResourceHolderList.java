package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.Measure;

// LUKETODO:  top level record
public final class MeasurePlusNpmResourceHolderList {

    private final List<MeasureOrNpmResourceHolder> measuresPlusNpmResourceHolders;

    public static MeasurePlusNpmResourceHolderList of(MeasureOrNpmResourceHolder measureOrNpmResourceHolder) {
        return new MeasurePlusNpmResourceHolderList(List.of(measureOrNpmResourceHolder));
    }

    public static MeasurePlusNpmResourceHolderList of(List<MeasureOrNpmResourceHolder> measureOrNpmResourceHolders) {
        return new MeasurePlusNpmResourceHolderList(measureOrNpmResourceHolders);
    }

    public static MeasurePlusNpmResourceHolderList of(Measure measure) {
        return new MeasurePlusNpmResourceHolderList(List.of(MeasureOrNpmResourceHolder.measureOnly(measure)));
    }

    public static MeasurePlusNpmResourceHolderList ofMeasures(List<Measure> measures) {
        return new MeasurePlusNpmResourceHolderList(
                measures.stream().map(MeasureOrNpmResourceHolder::measureOnly).toList());
    }

    private MeasurePlusNpmResourceHolderList(List<MeasureOrNpmResourceHolder> measuresPlusNpmResourceHolders) {
        this.measuresPlusNpmResourceHolders = measuresPlusNpmResourceHolders;
    }

    public List<MeasureOrNpmResourceHolder> getMeasuresPlusNpmResourceHolders() {
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (MeasurePlusNpmResourceHolderList) obj;
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
