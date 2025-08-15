package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.Measure;

// LUKETODO:  top level record
public final class MeasurePlusNpmResourceHolderList {

    private final List<MeasurePlusNpmResourceHolder> measuresPlusNpmResourceHolders;

    public static MeasurePlusNpmResourceHolderList of(MeasurePlusNpmResourceHolder measurePlusNpmResourceHolder) {
        return new MeasurePlusNpmResourceHolderList(List.of(measurePlusNpmResourceHolder));
    }

    public static MeasurePlusNpmResourceHolderList of(
            List<MeasurePlusNpmResourceHolder> measurePlusNpmResourceHolders) {
        return new MeasurePlusNpmResourceHolderList(measurePlusNpmResourceHolders);
    }

    public static MeasurePlusNpmResourceHolderList of(Measure measure) {
        return new MeasurePlusNpmResourceHolderList(List.of(MeasurePlusNpmResourceHolder.measureOnly(measure)));
    }

    public static MeasurePlusNpmResourceHolderList ofMeasures(List<Measure> measures) {
        return new MeasurePlusNpmResourceHolderList(
                measures.stream().map(MeasurePlusNpmResourceHolder::measureOnly).toList());
    }

    private MeasurePlusNpmResourceHolderList(List<MeasurePlusNpmResourceHolder> measuresPlusNpmResourceHolders) {
        this.measuresPlusNpmResourceHolders = measuresPlusNpmResourceHolders;
    }

    public List<MeasurePlusNpmResourceHolder> getMeasuresPlusNpmResourceHolders() {
        return measuresPlusNpmResourceHolders;
    }

    List<Measure> measures() {
        return this.measuresPlusNpmResourceHolders.stream()
                .map(MeasurePlusNpmResourceHolder::getMeasure)
                .toList();
    }

    public List<NpmResourceHolder> npmResourceHolders() {
        return this.measuresPlusNpmResourceHolders.stream()
                .map(MeasurePlusNpmResourceHolder::npmResourceHolder)
                .toList();
    }

    public List<Measure> getMeasures() {
        return measuresPlusNpmResourceHolders.stream()
                .map(MeasurePlusNpmResourceHolder::getMeasure)
                .toList();
    }

    public void checkMeasureLibraries() {
        for (MeasurePlusNpmResourceHolder measurePlusNpmResourceHolder : measuresPlusNpmResourceHolders) {
            if (!measurePlusNpmResourceHolder.hasLibrary()) {
                throw new InvalidRequestException("Measure %s does not have a primary library specified"
                        .formatted(measurePlusNpmResourceHolder.getMeasureUrl()));
            }
        }
    }

    public int size() {
        return measuresPlusNpmResourceHolders.size();
    }

    public List<MeasurePlusNpmResourceHolder> measuresPlusNpmResourceHolders() {
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
